package com.propertycrm.app

import android.app.Application
import com.propertycrm.app.ai.AICopilotEngine
import com.propertycrm.app.ai.AIGatewayClient
import com.propertycrm.app.ai.CallRecordingManager
import com.propertycrm.app.ai.SmartDocumentImporter
import com.propertycrm.app.data.local.AppDatabase
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.repository.BackupRepository
import com.propertycrm.app.data.repository.CustomerRepository
import com.propertycrm.app.data.repository.FollowUpRepository
import com.propertycrm.app.notification.ReminderNotificationManager
import com.propertycrm.app.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PropertyCrmApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var customerRepository: CustomerRepository
        private set

    lateinit var followUpRepository: FollowUpRepository
        private set

    lateinit var backupRepository: BackupRepository
        private set

    lateinit var preferenceManager: PreferenceManager
        private set

    lateinit var reminderScheduler: ReminderScheduler
        private set

    lateinit var reminderNotificationManager: ReminderNotificationManager
        private set

    lateinit var aiClient: AIGatewayClient
        private set

    lateinit var aiCopilotEngine: AICopilotEngine
        private set

    lateinit var smartDocumentImporter: SmartDocumentImporter
        private set

    lateinit var callRecordingManager: CallRecordingManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Database & DAOs
        database = AppDatabase.getInstance(this)
        val customerDao = database.customerDao()
        val followUpDao = database.followUpDao()
        val siteVisitDao = database.siteVisitDao()
        val customerInteractionDao = database.customerInteractionDao()

        // Initialize Preferences & Repositories
        preferenceManager = PreferenceManager(this)
        customerRepository = CustomerRepository(customerDao, followUpDao, siteVisitDao, customerInteractionDao)
        followUpRepository = FollowUpRepository(followUpDao, customerInteractionDao)
        backupRepository = BackupRepository(database)

        // Initialize Notification & Scheduler
        reminderNotificationManager = ReminderNotificationManager(this)
        reminderScheduler = ReminderScheduler(this)

        // Initialize AI Gateway, Copilot Agent, Ingestion & Call Recording
        aiClient = AIGatewayClient(preferenceManager)
        aiCopilotEngine = AICopilotEngine(aiClient, customerRepository, followUpRepository, database)
        smartDocumentImporter = SmartDocumentImporter(this, aiClient, customerRepository)
        callRecordingManager = CallRecordingManager(this, aiClient, customerRepository, preferenceManager)

        // Create Notification Channel
        reminderNotificationManager.createNotificationChannel()

        // Ensure daily reminder alarm is active on app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderScheduler.scheduleNextDailyReminder()
            } catch (e: Exception) {
                // Fail-safe initialization
            }
        }
    }
}
