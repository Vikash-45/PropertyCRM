package com.propertycrm.app.data.repository

import com.propertycrm.app.data.local.CustomerInteractionDao
import com.propertycrm.app.data.local.FollowUpDao
import com.propertycrm.app.data.model.CustomerInteraction
import com.propertycrm.app.data.model.FollowUp
import com.propertycrm.app.data.model.FollowUpStatus
import com.propertycrm.app.data.model.FollowUpWithCustomer
import com.propertycrm.app.data.model.InteractionType
import com.propertycrm.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

data class NotificationSummary(
    val uniqueTodayCustomers: Int,
    val totalTodayTasks: Int,
    val uniqueOverdueCustomers: Int,
    val totalOverdueTasks: Int
)

class FollowUpRepository(
    private val followUpDao: FollowUpDao,
    private val customerInteractionDao: CustomerInteractionDao
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

    // Distinct metrics for Dashboard (Item 7)
    fun countTodayUniqueCustomers(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countTodayUniqueCustomersFlow(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
    }

    fun countTodayTotalTasks(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countTodayTotalTasksFlow(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
    }

    fun countOverdueUniqueCustomers(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countOverdueUniqueCustomersFlow(DateUtils.getStartOfDay(now))
    }

    fun countOverdueTotalTasks(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countOverdueTotalTasksFlow(DateUtils.getStartOfDay(now))
    }

    fun countCompletedTasks(): Flow<Int> = followUpDao.countCompletedTasksFlow()

    fun countUpcomingTasks(): Flow<Int> {
        val now = System.currentTimeMillis()
        return followUpDao.countUpcomingTasksFlow(DateUtils.getEndOfDay(now))
    }

    /**
     * Synchronous check for notification evaluation (Item 8)
     */
    suspend fun getNotificationSummary(now: Long = System.currentTimeMillis()): NotificationSummary {
        val startOfDay = DateUtils.getStartOfDay(now)
        val endOfDay = DateUtils.getEndOfDay(now)
        val uniqueToday = followUpDao.countTodayUniqueCustomersSync(startOfDay, endOfDay)
        val totalToday = followUpDao.countTodayTotalTasksSync(startOfDay, endOfDay)
        val uniqueOverdue = followUpDao.countOverdueUniqueCustomersSync(startOfDay)
        val totalOverdue = followUpDao.countOverdueTotalTasksSync(startOfDay)
        return NotificationSummary(uniqueToday, totalToday, uniqueOverdue, totalOverdue)
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
        val id = followUpDao.insertFollowUp(followUp)

        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.FOLLOW_UP_CREATED,
                notes = "Scheduled follow-up for ${DateUtils.formatDate(dateMillis)}${if (notes.isNotBlank()) ": $notes" else ""}"
            )
        )
        return id
    }

    suspend fun markCompleted(id: Long, completionNotes: String = "") {
        val fu = followUpDao.getFollowUpByIdSync(id)
        followUpDao.markCompleted(id, completionNotes.trim(), System.currentTimeMillis())

        if (fu != null) {
            customerInteractionDao.insertInteraction(
                CustomerInteraction(
                    customerId = fu.customerId,
                    type = InteractionType.NOTE,
                    notes = "Completed follow-up from ${DateUtils.formatDate(fu.followUpDate)}${if (completionNotes.isNotBlank()) ": $completionNotes" else ""}"
                )
            )
        }
    }

    /**
     * Reschedules follow-up:
     * Completes or cancels old pending task with explicit rescheduled status and creates a new task (Item 6).
     * Preserves entire historical trail.
     */
    suspend fun rescheduleFollowUp(
        existingFollowUpId: Long,
        newDateMillis: Long,
        newTime: String?,
        reasonNotes: String
    ): Long {
        val existing = followUpDao.getFollowUpByIdSync(existingFollowUpId)
        val customerId = existing?.customerId ?: return 0L

        val rescheduleNote = if (reasonNotes.isNotBlank()) {
            "Rescheduled: $reasonNotes"
        } else {
            "Rescheduled to ${DateUtils.formatDate(newDateMillis)}"
        }

        // Cancel previous pending task with explicit note
        followUpDao.markCancelled(existingFollowUpId, rescheduleNote)

        // Insert new pending follow-up task
        val newFollowUp = FollowUp(
            customerId = customerId,
            followUpDate = DateUtils.getStartOfDay(newDateMillis),
            followUpTime = newTime?.takeIf { it.isNotBlank() },
            notes = reasonNotes.trim(),
            status = FollowUpStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        val newId = followUpDao.insertFollowUp(newFollowUp)

        customerInteractionDao.insertInteraction(
            CustomerInteraction(
                customerId = customerId,
                type = InteractionType.RESCHEDULED,
                notes = "Follow-up rescheduled to ${DateUtils.formatDate(newDateMillis)}${if (reasonNotes.isNotBlank()) " ($reasonNotes)" else ""}"
            )
        )
        return newId
    }

    suspend fun deleteFollowUp(id: Long) {
        followUpDao.deleteFollowUpById(id)
    }
}
