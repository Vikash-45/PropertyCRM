package com.propertycrm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.propertycrm.app.notification.ReminderNotificationManager
import com.propertycrm.app.ui.navigation.AppNavigation
import com.propertycrm.app.ui.navigation.Screen
import com.propertycrm.app.ui.theme.PropertyCRMTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Graceful handling, whether granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+ if not already granted
        checkAndRequestNotificationPermission()

        val appContainer = application as PropertyCrmApp

        setContent {
            PropertyCRMTheme {
                val navController = rememberNavController()

                // Handle intent extras from notification tap
                androidx.compose.runtime.LaunchedEffect(intent) {
                    handleNotificationIntent(intent, navController)
                }

                AppNavigation(
                    navController = navController,
                    appContainer = appContainer
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?, navController: androidx.navigation.NavHostController) {
        if (intent == null) return
        val destination = intent.getStringExtra(ReminderNotificationManager.EXTRA_NAVIGATE_TO)
        val filter = intent.getStringExtra(ReminderNotificationManager.EXTRA_FILTER) ?: "TODAY"

        if (destination == ReminderNotificationManager.DESTINATION_FOLLOW_UPS) {
            navController.navigate(Screen.FollowUps.createRoute(filter)) {
                popUpTo(Screen.Dashboard.route) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
