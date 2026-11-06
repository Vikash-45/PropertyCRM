package com.propertycrm.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.propertycrm.app.data.model.AppointmentStatus
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.data.model.SiteVisitWithCustomer
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteVisitDao {

    @Query("SELECT * FROM site_visits WHERE customer_id = :customerId ORDER BY visit_date DESC, id DESC")
    fun getSiteVisitsForCustomer(customerId: Long): Flow<List<SiteVisit>>

    @Transaction
    @Query("""
        SELECT s.* FROM site_visits s
        INNER JOIN customers c ON s.customer_id = c.id
        WHERE c.is_archived = 0 AND s.status = 'SCHEDULED' AND s.visit_date >= :startOfDay
        ORDER BY s.visit_date ASC, s.id ASC
    """)
    fun getUpcomingSiteVisits(startOfDay: Long): Flow<List<SiteVisitWithCustomer>>

    @Query("""
        SELECT COUNT(*) FROM site_visits s
        INNER JOIN customers c ON s.customer_id = c.id
        WHERE c.is_archived = 0 AND s.status = 'SCHEDULED' AND s.visit_date >= :startOfDay
    """)
    fun countUpcomingSiteVisitsFlow(startOfDay: Long): Flow<Int>

    @Query("SELECT * FROM site_visits ORDER BY id ASC")
    suspend fun getAllSiteVisitsList(): List<SiteVisit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiteVisit(siteVisit: SiteVisit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(siteVisits: List<SiteVisit>): List<Long>

    @Update
    suspend fun updateSiteVisit(siteVisit: SiteVisit)

    @Query("UPDATE site_visits SET status = :status, notes = CASE WHEN :notes != '' THEN :notes ELSE notes END WHERE id = :id")
    suspend fun updateStatus(id: Long, status: AppointmentStatus, notes: String = "")

    @Query("DELETE FROM site_visits WHERE id = :id")
    suspend fun deleteSiteVisitById(id: Long)
}
