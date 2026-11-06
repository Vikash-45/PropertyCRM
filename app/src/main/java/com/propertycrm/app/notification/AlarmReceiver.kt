package com.propertycrm.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.propertycrm.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationManager = ReminderNotificationManager(context)

                when (intent?.action) {
                    ReminderScheduler.ACTION_SITE_VISIT_PRIOR_ALERT -> {
                        val customerId = intent.getLongExtra(ReminderScheduler.EXTRA_CUSTOMER_ID, 0L)
                        val visitId = intent.getLongExtra(ReminderScheduler.EXTRA_SITE_VISIT_ID, 0L)
                        val leadTimeMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_TIME_MINUTES, 60)

                        if (customerId != 0L && visitId != 0L) {
                            val db = AppDatabase.getInstance(context)
                            val customer = db.customerDao().getCustomerById(customerId).firstOrNull()
                            val siteVisit = db.siteVisitDao().getSiteVisitById(visitId)

                            if (customer != null && siteVisit != null) {
                                notificationManager.showPriorSiteVisitNotification(
                                    customer = customer,
                                    siteVisit = siteVisit,
                                    minutesBefore = leadTimeMinutes
                                )
                            }
                        }
                    }

                    else -> {
                        // Standard Daily Reminder
                        notificationManager.checkAndShowDailyReminderNotification(forceShowTest = false)
                        val scheduler = ReminderScheduler(context)
                        scheduler.scheduleNextDailyReminder()
                    }
                }
            } catch (e: Exception) {
                // Graceful error recovery
            } finally {
                pendingResult.finish()
            }
        }
    }
}
