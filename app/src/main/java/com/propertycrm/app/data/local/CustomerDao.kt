package com.propertycrm.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.CustomerStage
import com.propertycrm.app.data.model.CustomerWithFollowUps
import com.propertycrm.app.data.model.PropertyType
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE is_archived = 0 ORDER BY updated_at DESC")
    fun getAllActiveCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE is_archived = 1 ORDER BY updated_at DESC")
    fun getAllArchivedCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdSync(id: Long): Customer?

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerWithFollowUps(id: Long): Flow<CustomerWithFollowUps?>

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 10")
    suspend fun findCustomersByPhone(phone: String): List<Customer>

    @Query("SELECT COUNT(*) FROM customers WHERE is_archived = 0")
    fun countActiveCustomers(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE is_archived = 0")
    suspend fun countActiveCustomersSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>): List<Long>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("UPDATE customers SET is_archived = :archived, updated_at = :timestamp WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET is_do_not_call = :doNotCall, updated_at = :timestamp WHERE id = :id")
    suspend fun setDoNotCall(id: Long, doNotCall: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET stage = :stage, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStage(id: Long, stage: CustomerStage, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET reminder_lead_time_minutes = :leadTimeMinutes, notifications_enabled = :enabled, custom_notification_notes = :notes, updated_at = :timestamp WHERE id = :id")
    suspend fun updateNotificationSettings(
        id: Long,
        leadTimeMinutes: Int,
        enabled: Boolean,
        notes: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT * FROM customers 
        WHERE is_archived = 0 AND (
            name LIKE '%' || :query || '%' OR 
            phone LIKE '%' || :query || '%' OR 
            preferred_location LIKE '%' || :query || '%' OR
            inquiry_reason LIKE '%' || :query || '%'
        )
        ORDER BY updated_at DESC
    """)
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("""
        SELECT * FROM customers 
        WHERE is_archived = 0 
          AND (:stage IS NULL OR stage = :stage)
          AND (:propType IS NULL OR property_type = :propType)
          AND (:location = '' OR preferred_location LIKE '%' || :location || '%')
          AND (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
    """)
    fun filterCustomers(
        query: String,
        stage: CustomerStage?,
        propType: PropertyType?,
        location: String
    ): Flow<List<Customer>>
}
