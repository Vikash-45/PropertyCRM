package com.propertycrm.app.data.repository

import com.propertycrm.app.data.local.FollowUpDao
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

class FollowUpRepository(
    private val followUpDao: FollowUpDao
) {

    fun getFollowUpsForCustomer(customerId: Long): Flow<List<FollowUp>> =
        followUpDao.getFollowUpsForCustomer(customerId)

    fun getTodayFollowUps(): Flow<List<FollowUpWithCustomer>> {
        val now = System.currentTimeMillis()
        return followUpDao.getTodayFollowUps(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
    }

    fun getOverdueFollowUps(): Flow<List<FollowUpWithCustomer>> {
        val now = System.currentTimeMillis()
        return followUpDao.getOverdueFollowUps(DateUtils.getStartOfDay(now))
    }

    fun getUpcomingFollowUps(): Flow<List<FollowUpWithCustomer>> {
        val now = System.currentTimeMillis()
        return followUpDao.getUpcomingFollowUps(DateUtils.getEndOfDay(now))
    }

    fun getCompletedFollowUps(): Flow<List<FollowUpWithCustomer>> =
        followUpDao.getCompletedFollowUps()

    fun getAllFollowUps(): Flow<List<FollowUpWithCustomer>> =
        followUpDao.getAllFollowUps()

    // Counts for Dashboard
    fun countTodayPending(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countTodayPendingFlow(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
    }

    fun countOverdue(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countOverdueFlow(DateUtils.getStartOfDay(now))
    }

    fun countUpcoming(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countUpcomingFlow(DateUtils.getEndOfDay(now))
    }

    fun countCompleted(): Flow<Int> = followUpDao.countCompletedFlow()

    /**
     * Synchronous check for notification evaluation:
     * Returns Pair(todayPendingCount, overdueCount)
     */
    suspend fun getNotificationCounts(now: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val startOfDay = DateUtils.getStartOfDay(now)
        val endOfDay = DateUtils.getEndOfDay(now)
        val todayPending = followUpDao.countTodayPendingSync(startOfDay, endOfDay)
        val overdue = followUpDao.countOverdueSync(startOfDay)
        return Pair(todayPending, overdue)
    }

    suspend fun addFollowUp(
        customerId: Long,
        dateMillis: Long,
        time: String? = null,
        notes: String = ""
    ): Long {
        val followUp = FollowUp(
            customerId = customerId,
            followUpDate = DateUtils.getStartOfDay(dateMillis),
            followUpTime = time?.takeIf { it.isNotBlank() },
            notes = notes.trim(),
            status = FollowUpStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        return followUpDao.insertFollowUp(followUp)
    }

    suspend fun markCompleted(id: Long, completionNotes: String = "") {
        followUpDao.markCompleted(id, completionNotes.trim(), System.currentTimeMillis())
    }

    /**
     * Reschedules a follow-up. Preserves history:
     * Marks previous pending follow-up as completed/cancelled with note, and inserts a new pending follow-up!
     */
    suspend fun rescheduleFollowUp(
        existingFollowUpId: Long,
        newDateMillis: Long,
        newTime: String?,
        reasonNotes: String
    ): Long {
        val existing = followUpDao.getFollowUpByIdSync(existingFollowUpId)
        val customerId = existing?.customerId ?: return 0L

        // Mark existing follow up with note indicating it was rescheduled
        val rescheduleNote = if (reasonNotes.isNotBlank()) {
            "Rescheduled: $reasonNotes"
        } else {
            "Rescheduled to ${DateUtils.formatDate(newDateMillis)}"
        }
        followUpDao.markCompleted(existingFollowUpId, rescheduleNote, System.currentTimeMillis())

        // Insert new pending record to keep full chronological history
        val newFollowUp = FollowUp(
            customerId = customerId,
            followUpDate = DateUtils.getStartOfDay(newDateMillis),
            followUpTime = newTime?.takeIf { it.isNotBlank() },
            notes = reasonNotes.trim(),
            status = FollowUpStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        return followUpDao.insertFollowUp(newFollowUp)
    }

    suspend fun deleteFollowUp(id: Long) {
        followUpDao.deleteFollowUpById(id)
    }
}
