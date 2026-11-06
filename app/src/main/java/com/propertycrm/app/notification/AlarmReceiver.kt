package com.propertycrm.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Show notification if there are follow-ups due/overdue
                val notificationManager = ReminderNotificationManager(context)
                notificationManager.checkAndShowDailyReminderNotification(forceShowTest = false)

                // Requirement 7: Schedule next reminder after processing current reminder
                val scheduler = ReminderScheduler(context)
                scheduler.scheduleNextDailyReminder()
            } catch (e: Exception) {
                // Graceful error recovery
            } finally {
                pendingResult.finish()
            }
        }
    }
}
