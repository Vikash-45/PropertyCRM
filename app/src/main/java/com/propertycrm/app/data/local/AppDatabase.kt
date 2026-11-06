package com.propertycrm.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.SiteVisit

@Database(
    entities = [
        Customer::class,
        FollowUp::class,
        SiteVisit::class,
        CustomerInteraction::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun siteVisitDao(): SiteVisitDao
    abstract fun customerInteractionDao(): CustomerInteractionDao

    companion object {
        private const val DATABASE_NAME = "property_crm_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to customers table
                db.execSQL("ALTER TABLE customers ADD COLUMN requirement_type TEXT NOT NULL DEFAULT 'BUY'")
                db.execSQL("ALTER TABLE customers ADD COLUMN preferred_size TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN preferred_contact_time TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN next_action TEXT NOT NULL DEFAULT 'CALL'")
                db.execSQL("ALTER TABLE customers ADD COLUMN assigned_person TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN is_do_not_call INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_is_do_not_call ON customers(is_do_not_call)")

                // Create site_visits table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS site_visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customer_id INTEGER NOT NULL,
                        property_location TEXT NOT NULL,
                        visit_date INTEGER NOT NULL,
                        visit_time TEXT,
                        status TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_site_visits_customer_id ON site_visits(customer_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_site_visits_visit_date ON site_visits(visit_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_site_visits_status ON site_visits(status)")

                // Create customer_interactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS customer_interactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customer_id INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        call_outcome TEXT,
                        notes TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_interactions_customer_id ON customer_interactions(customer_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_interactions_timestamp ON customer_interactions(timestamp)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
