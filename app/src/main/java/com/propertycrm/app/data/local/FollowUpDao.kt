package com.propertycrm.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.FollowUpWithCustomer
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {

    @Query("SELECT * FROM follow_ups WHERE customer_id = :customerId ORDER BY follow_up_date DESC, id DESC")
    fun getFollowUpsForCustomer(customerId: Long): Flow<List<FollowUp>>

    @Query("SELECT * FROM follow_ups WHERE id = :id LIMIT 1")
    fun getFollowUpById(id: Long): Flow<FollowUp?>

    @Query("SELECT * FROM follow_ups WHERE id = :id LIMIT 1")
    suspend fun getFollowUpByIdSync(id: Long): FollowUp?

    @Query("SELECT * FROM follow_ups ORDER BY id ASC")
    suspend fun getAllFollowUpsList(): List<FollowUp>

    // FollowUpWithCustomer queries for the 5 filter tabs
    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
        ORDER BY f.follow_up_date ASC, f.id ASC
    """)
    fun getTodayFollowUps(startOfDay: Long, endOfDay: Long): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
        ORDER BY f.follow_up_date ASC, f.id ASC
    """)
    fun getOverdueFollowUps(startOfDay: Long): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date > :endOfDay
        ORDER BY f.follow_up_date ASC, f.id ASC
    """)
    fun getUpcomingFollowUps(endOfDay: Long): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE f.status = 'COMPLETED'
        ORDER BY f.completed_at DESC, f.follow_up_date DESC
    """)
    fun getCompletedFollowUps(): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0
        ORDER BY f.follow_up_date DESC, f.id DESC
    """)
    fun getAllFollowUps(): Flow<List<FollowUpWithCustomer>>

    // Counts for Dashboard
    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    fun countTodayPendingFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    suspend fun countTodayPendingSync(startOfDay: Long, endOfDay: Long): Int

    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    fun countOverdueFlow(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    suspend fun countOverdueSync(startOfDay: Long): Int

    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND f.status = 'PENDING' 
          AND f.follow_up_date > :endOfDay
    """)
    fun countUpcomingFlow(endOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE f.status = 'COMPLETED'
    """)
    fun countCompletedFlow(): Flow<Int>

    // Mutations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUp): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(followUps: List<FollowUp>): List<Long>

    @Update
    suspend fun updateFollowUp(followUp: FollowUp)

    @Delete
    suspend fun deleteFollowUp(followUp: FollowUp)

    @Query("DELETE FROM follow_ups WHERE id = :id")
    suspend fun deleteFollowUpById(id: Long)

    @Query("""
        UPDATE follow_ups 
        SET status = 'COMPLETED', notes = CASE WHEN :completionNotes != '' THEN :completionNotes ELSE notes END, completed_at = :completedAt 
        WHERE id = :id
    """)
    suspend fun markCompleted(id: Long, completionNotes: String = "", completedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE follow_ups 
        SET follow_up_date = :newDate, follow_up_time = :newTime, notes = :notes, status = 'PENDING'
        WHERE id = :id
    """)
    suspend fun updateSchedule(id: Long, newDate: Long, newTime: String?, notes: String)
}
