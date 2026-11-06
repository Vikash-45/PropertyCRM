package com.propertycrm.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.util.DateUtils
import kotlinx.coroutines.flow.first

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val preferenceManager = PreferenceManager(context)

    companion object {
        const val ALARM_REQUEST_CODE = 2001
        const val ACTION_DAILY_REMINDER = "com.propertycrm.app.ACTION_DAILY_REMINDER"
    }

    /**
     * Checks whether the application has permission to schedule exact alarms.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Schedules the next daily alarm based on user preferences.
     * Uses exact alarm if permitted; otherwise falls back to inexact without crashing.
     */
    suspend fun scheduleNextDailyReminder(fromMillis: Long = System.currentTimeMillis()) {
        val enabled = preferenceManager.isReminderEnabled.first()
        if (!enabled || alarmManager == null) {
            cancelReminder()
            return
        }

        val hour = preferenceManager.reminderHour.first()
        val minute = preferenceManager.reminderMinute.first()
        val triggerAtMillis = DateUtils.calculateNextReminderTime(hour, minute, fromMillis)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Inexact fallback (Requirement 5)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In case of permission revocation between check and call
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels any pending reminder alarms.
     */
    fun cancelReminder() {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
