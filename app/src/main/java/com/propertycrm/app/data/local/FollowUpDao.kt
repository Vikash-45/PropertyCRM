package com.propertycrm.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.propertycrm.app.data.model.FollowUp
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

    // FollowUpWithCustomer queries with strict DNC / WON / LOST / ARCHIVED exclusion for active calls
    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
        ORDER BY f.follow_up_date ASC, f.id ASC
    """)
    fun getTodayFollowUps(startOfDay: Long, endOfDay: Long): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
        ORDER BY f.follow_up_date ASC, f.id ASC
    """)
    fun getOverdueFollowUps(startOfDay: Long): Flow<List<FollowUpWithCustomer>>

    @Transaction
    @Query("""
        SELECT f.* FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
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
        ORDER BY f.follow_up_date DESC, f.id DESC
    """)
    fun getAllFollowUps(): Flow<List<FollowUpWithCustomer>>

    // DISTINCT COUNTS for Dashboard (Item 7)
    // 1. Unique customers due today
    @Query("""
        SELECT COUNT(DISTINCT c.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    fun countTodayUniqueCustomersFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    // 2. Total follow-up tasks due today
    @Query("""
        SELECT COUNT(f.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    fun countTodayTotalTasksFlow(startOfDay: Long, endOfDay: Long): Flow<Int>

    // 3. Unique overdue customers
    @Query("""
        SELECT COUNT(DISTINCT c.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    fun countOverdueUniqueCustomersFlow(startOfDay: Long): Flow<Int>

    // 4. Total overdue tasks
    @Query("""
        SELECT COUNT(f.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    fun countOverdueTotalTasksFlow(startOfDay: Long): Flow<Int>

    // 5. Total completed follow-up tasks
    @Query("SELECT COUNT(*) FROM follow_ups WHERE status = 'COMPLETED'")
    fun countCompletedTasksFlow(): Flow<Int>

    // Synchronous counts for daily notification evaluation
    @Query("""
        SELECT COUNT(DISTINCT c.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    suspend fun countTodayUniqueCustomersSync(startOfDay: Long, endOfDay: Long): Int

    @Query("""
        SELECT COUNT(f.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date >= :startOfDay AND f.follow_up_date <= :endOfDay
    """)
    suspend fun countTodayTotalTasksSync(startOfDay: Long, endOfDay: Long): Int

    @Query("""
        SELECT COUNT(DISTINCT c.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    suspend fun countOverdueUniqueCustomersSync(startOfDay: Long): Int

    @Query("""
        SELECT COUNT(f.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date < :startOfDay
    """)
    suspend fun countOverdueTotalTasksSync(startOfDay: Long): Int

    // Upcoming tasks count
    @Query("""
        SELECT COUNT(f.id) FROM follow_ups f
        INNER JOIN customers c ON f.customer_id = c.id
        WHERE c.is_archived = 0 AND c.is_do_not_call = 0 AND c.stage NOT IN ('WON', 'LOST')
          AND f.status = 'PENDING' 
          AND f.follow_up_date > :endOfDay
    """)
    fun countUpcomingTasksFlow(endOfDay: Long): Flow<Int>

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
        SET status = 'CANCELLED', notes = CASE WHEN :cancellationNotes != '' THEN :cancellationNotes ELSE notes END 
        WHERE id = :id
    """)
    suspend fun markCancelled(id: Long, cancellationNotes: String = "")

    @Query("""
        UPDATE follow_ups 
        SET follow_up_date = :newDate, follow_up_time = :newTime, notes = :notes, status = 'PENDING'
        WHERE id = :id
    """)
    suspend fun updateSchedule(id: Long, newDate: Long, newTime: String?, notes: String)
}
