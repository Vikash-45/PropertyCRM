package com.propertycrm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.propertycrm.app.notification.ReminderNotificationManager
import com.propertycrm.app.ui.navigation.AppNavigation
import com.propertycrm.app.ui.navigation.Screen
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.PropertyCRMTheme
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.ui.theme.getDarkTextFieldColors
import com.propertycrm.app.util.SecurityUtils

class MainActivity : ComponentActivity() {

    private val currentIntentState = mutableStateOf<Intent?>(null)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntentState.value = intent
        checkAndRequestNotificationPermission()

        val appContainer = application as PropertyCrmApp

        setContent {
            PropertyCRMTheme {
                val isPinLockEnabled by appContainer.preferenceManager.isPinLockEnabled.collectAsState(initial = null)
                val pinHash by appContainer.preferenceManager.pinHash.collectAsState(initial = null)
                val pinSalt by appContainer.preferenceManager.pinSalt.collectAsState(initial = null)

                var sessionUnlocked by remember { mutableStateOf(false) }

                if (isPinLockEnabled == null) {
                    // Loading preferences from DataStore
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else if (isPinLockEnabled == true && !sessionUnlocked) {
                    if (pinHash != null && pinSalt != null) {
                        PinUnlockScreen(
                            targetHash = pinHash!!,
                            salt = pinSalt!!,
                            onUnlocked = { sessionUnlocked = true }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    val activeIntent by currentIntentState

                    androidx.compose.runtime.LaunchedEffect(activeIntent) {
                        handleNotificationIntent(activeIntent, navController)
                    }

                    AppNavigation(
                        navController = navController,
                        appContainer = appContainer
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
    }

    private fun handleNotificationIntent(intent: Intent?, navController: androidx.navigation.NavHostController) {
        if (intent == null) return
        val destination = intent.getStringExtra(ReminderNotificationManager.EXTRA_NAVIGATE_TO)

        if (destination == ReminderNotificationManager.DESTINATION_DAILY_SUMMARY) {
            navController.navigate(Screen.DailySummary.route) {
                popUpTo(Screen.Dashboard.route) { saveState = true }
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

@Composable
fun PinUnlockScreen(
    targetHash: String,
    salt: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Property Follow-up CRM", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Enter your PIN to unlock CRM records", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                        enteredPin = it
                        errorText = null
                        if (it.length >= 4) {
                            val computedHash = SecurityUtils.hashPin(it, salt)
                            if (computedHash == targetHash) {
                                onUnlocked()
                            }
                        }
                    }
                },
                label = { Text("4-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = getDarkTextFieldColors(),
                singleLine = true
            )

            if (errorText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorText!!, color = StatusOverdue, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val computedHash = SecurityUtils.hashPin(enteredPin, salt)
                    if (computedHash == targetHash) {
                        onUnlocked()
                    } else {
                        errorText = "Incorrect PIN. Try again."
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Unlock", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
