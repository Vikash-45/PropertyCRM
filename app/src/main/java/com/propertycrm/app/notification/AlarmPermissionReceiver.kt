package com.propertycrm.app.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (intent?.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val scheduler = ReminderScheduler(context)
                        scheduler.scheduleNextDailyReminder()
                    } catch (e: Exception) {
                        // Safe execution
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
