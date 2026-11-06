package com.propertycrm.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.propertycrm.app.MainActivity
import com.propertycrm.app.R
import com.propertycrm.app.data.local.AppDatabase
import com.propertycrm.app.data.repository.FollowUpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "property_followup_reminders"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_NAVIGATE_TO = "NAVIGATE_TO"
        const val DESTINATION_DAILY_SUMMARY = "daily_summary"
    }

    init {
        createNotificationChannel()
    }

    fun areNotificationsEnabled(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = systemManager?.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return true
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks today's and overdue follow-up counts and unique customer counts.
     * Cancels / suppresses notification if no eligible tasks remain.
     */
    suspend fun checkAndShowDailyReminderNotification(forceShowTest: Boolean = false) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val repository = FollowUpRepository(database.followUpDao(), database.customerInteractionDao())

        val summary = repository.getNotificationSummary()
        val totalPendingTasks = summary.totalTodayTasks + summary.totalOverdueTasks

        // Suppress notification if 0 eligible tasks (Item 10)
        if (!forceShowTest && totalPendingTasks == 0) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return@withContext
        }

        val testPrefix = if (forceShowTest && totalPendingTasks == 0) "[Test] " else ""

        val bodyText = when {
            summary.totalTodayTasks > 0 && summary.totalOverdueTasks > 0 -> {
                "${summary.uniqueTodayCustomers} customer(s) due today (${summary.totalTodayTasks} calls). ${summary.uniqueOverdueCustomers} overdue (${summary.totalOverdueTasks} calls)."
            }
            summary.totalTodayTasks > 0 -> {
                "${summary.uniqueTodayCustomers} customer(s) need follow-up today (${summary.totalTodayTasks} total tasks)."
            }
            summary.totalOverdueTasks > 0 -> {
                "${summary.uniqueOverdueCustomers} customer(s) are overdue for calls (${summary.totalOverdueTasks} total tasks)."
            }
            else -> {
                "Notification system active. All follow-ups are up to date!"
            }
        }

        // Deep-link to Combined Daily Summary (Section A: Today + Section B: Overdue) (Item 9)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_DAILY_SUMMARY)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$testPrefix${context.getString(R.string.notification_title)}")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Review Calls",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission denied - safe handling
        }
    }
}
