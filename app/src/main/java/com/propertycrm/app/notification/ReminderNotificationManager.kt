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
        const val EXTRA_FILTER = "FILTER"
        const val DESTINATION_FOLLOW_UPS = "follow_ups"
        const val FILTER_TODAY = "TODAY"
    }

    init {
        createNotificationChannel()
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
     * Evaluates pending follow-ups due today and overdue.
     * If total is 0, suppresses notification.
     * Otherwise displays a grouped summary notification.
     */
    suspend fun checkAndShowDailyReminderNotification(forceShowTest: Boolean = false) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val repository = FollowUpRepository(database.followUpDao())

        val (todayCount, overdueCount) = repository.getNotificationCounts()

        // Requirement 13: Suppress the notification when there are no due or overdue customers
        if (!forceShowTest && todayCount == 0 && overdueCount == 0) {
            return@withContext
        }

        val testPrefix = if (forceShowTest && todayCount == 0 && overdueCount == 0) "[Test] " else ""

        val bodyText = when {
            todayCount > 0 && overdueCount > 0 -> {
                context.getString(R.string.notification_body_format, todayCount, overdueCount)
            }
            todayCount > 0 -> {
                context.getString(R.string.notification_body_today_only, todayCount)
            }
            overdueCount > 0 -> {
                context.getString(R.string.notification_body_overdue_only, overdueCount)
            }
            else -> {
                "Notification system active. All follow-ups are up to date!"
            }
        }

        // Tap action: Opens MainActivity with deep-link to Today's follow-ups
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_FOLLOW_UPS)
            putExtra(EXTRA_FILTER, FILTER_TODAY)
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
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Requirement 14: Never expose sensitive notes or phones on lock screen
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.notification_action_open),
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission denied - handle gracefully without crashing (Requirement 6)
        }
    }
}
