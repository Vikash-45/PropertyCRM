package com.propertycrm.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.model.Customer
import com.propertycrm.app.data.model.SiteVisit
import com.propertycrm.app.util.DateUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val preferenceManager = PreferenceManager(context)

    companion object {
        const val ALARM_REQUEST_CODE = 2001
        const val ACTION_DAILY_REMINDER = "com.propertycrm.app.ACTION_DAILY_REMINDER"
        const val ACTION_SITE_VISIT_PRIOR_ALERT = "com.propertycrm.app.ACTION_SITE_VISIT_PRIOR_ALERT"
        const val EXTRA_CUSTOMER_ID = "CUSTOMER_ID"
        const val EXTRA_SITE_VISIT_ID = "SITE_VISIT_ID"
        const val EXTRA_LEAD_TIME_MINUTES = "LEAD_TIME_MINUTES"
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

    /**
     * Schedules a custom prior alert alarm before an upcoming site visit.
     */
    fun schedulePriorSiteVisitAlert(customer: Customer, siteVisit: SiteVisit) {
        if (!customer.notificationsEnabled || alarmManager == null) return

        val leadTimeMinutes = customer.reminderLeadTimeMinutes
        val visitExactMillis = parseVisitTimeMillis(siteVisit.visitDate, siteVisit.visitTime)
        val triggerAtMillis = visitExactMillis - (leadTimeMinutes * 60 * 1000L)

        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SITE_VISIT_PRIOR_ALERT
            putExtra(EXTRA_CUSTOMER_ID, customer.id)
            putExtra(EXTRA_SITE_VISIT_ID, siteVisit.id)
            putExtra(EXTRA_LEAD_TIME_MINUTES, leadTimeMinutes)
        }

        val requestCode = (60000 + siteVisit.id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    fun cancelSiteVisitPriorAlert(siteVisitId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SITE_VISIT_PRIOR_ALERT
        }
        val requestCode = (60000 + siteVisitId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun parseVisitTimeMillis(visitDate: Long, visitTime: String?): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = visitDate }
        if (!visitTime.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val parsed = sdf.parse(visitTime.trim())
                if (parsed != null) {
                    val tCal = Calendar.getInstance().apply { time = parsed }
                    cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
