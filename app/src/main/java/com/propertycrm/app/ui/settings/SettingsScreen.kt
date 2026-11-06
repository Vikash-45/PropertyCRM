package com.propertycrm.app.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.propertycrm.app.R
import com.propertycrm.app.data.model.ImportResult
import com.propertycrm.app.ui.components.PasswordPromptDialog
import com.propertycrm.app.ui.theme.DarkBackground
import com.propertycrm.app.ui.theme.DarkBorder
import com.propertycrm.app.ui.theme.DarkSurface
import com.propertycrm.app.ui.theme.DarkSurfaceVariant
import com.propertycrm.app.ui.theme.PrimaryBlue
import com.propertycrm.app.ui.theme.StatusCompleted
import com.propertycrm.app.ui.theme.StatusOverdue
import com.propertycrm.app.ui.theme.TextMuted
import com.propertycrm.app.ui.theme.TextPrimary
import com.propertycrm.app.ui.theme.TextSecondary
import com.propertycrm.app.util.DateUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var importResultDialog by remember { mutableStateOf<ImportResult?>(null) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PermissionChecker.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Storage Access Framework Launchers
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportCsv(context, it) }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            pendingExportUri = uri
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.ShowImportResult -> {
                    importResultDialog = event.result
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Notifications & Reminders Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.notifications_reminders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily Reminders Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.enable_daily_reminders), color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = "Trigger grouped reminder for today's and overdue calls", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = uiState.isReminderEnabled,
                            onCheckedChange = { viewModel.toggleReminder(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reminder Time Setting (Default 9:00 AM)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute -> viewModel.updateReminderTime(hourOfDay, minute) },
                                    uiState.reminderHour,
                                    uiState.reminderMinute,
                                    false
                                ).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = stringResource(R.string.reminder_time), color = TextPrimary, fontSize = 13.sp)
                                Text(
                                    text = DateUtils.formatTime(uiState.reminderHour, uiState.reminderMinute),
                                    color = PrimaryBlue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(text = "Change", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notification Permission Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.notification_permission), color = TextPrimary, fontSize = 13.sp)
                            Text(
                                text = if (hasNotificationPermission) stringResource(R.string.granted) else stringResource(R.string.not_granted),
                                color = if (hasNotificationPermission) StatusCompleted else StatusOverdue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Exact Alarm Permission Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.exact_alarm_permission), color = TextPrimary, fontSize = 13.sp)
                            Text(
                                text = if (uiState.isExactAlarmPermitted) stringResource(R.string.granted) else "Inexact Fallback Active",
                                color = if (uiState.isExactAlarmPermitted) StatusCompleted else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (!uiState.isExactAlarmPermitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                            ) {
                                Text("Settings", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Test Notification Button
                    Button(
                        onClick = { viewModel.triggerTestNotification() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.test_notification), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Battery Troubleshooting Guide (Item 11)
                    OutlinedButton(
                        onClick = { showBatteryGuideDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = StatusUpcoming, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.battery_troubleshooting), fontSize = 12.sp)
                    }
                }
            }

            // Security & Privacy Card (Item 12 & 13)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Security & PIN Lock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.app_security_lock), color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = if (uiState.isPinLockEnabled) "PIN lock active" else "Protect CRM data from unauthorized access", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = uiState.isPinLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) showSetPinDialog = true
                                else viewModel.disablePin()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = StatusCompleted)
                        )
                    }
                }
            }

            // Data Backup & Export Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.data_backup_export),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Save encrypted database archives (AES-256-GCM) directly to Google Drive or local storage.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // CSV Export
                    SettingsActionButton(
                        icon = Icons.Default.FileDownload,
                        title = stringResource(R.string.export_csv),
                        subtitle = "Spreadsheet table for Excel / Google Sheets",
                        onClick = {
                            val defaultName = "PropertyCRM_Customers_${System.currentTimeMillis()}.csv"
                            exportCsvLauncher.launch(defaultName)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Encrypted JSON Backup (Item 13)
                    SettingsActionButton(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.export_json),
                        subtitle = "Password-protected AES-256 authenticated backup",
                        onClick = {
                            val defaultName = "PropertyCRM_Backup_${System.currentTimeMillis()}.pcrm"
                            exportJsonLauncher.launch(defaultName)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Restore Encrypted Backup
                    SettingsActionButton(
                        icon = Icons.Default.FileUpload,
                        title = stringResource(R.string.import_json),
                        subtitle = "Decrypt and restore data with integrity check",
                        onClick = {
                            importJsonLauncher.launch(arrayOf("*/*"))
                        }
                    )

                    if (uiState.lastBackupTime != null || uiState.lastRestoreTime != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (uiState.lastBackupTime != null) {
                            Text(text = "• Last Backup: ${DateUtils.formatDate(uiState.lastBackupTime)}", color = TextSecondary, fontSize = 11.sp)
                        }
                        if (uiState.lastRestoreTime != null) {
                            Text(text = "• Last Restore: ${DateUtils.formatDate(uiState.lastRestoreTime)}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // About Application Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_app),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.about_app_desc), color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "• Version: 1.1.0 (Production Release)", color = TextPrimary, fontSize = 12.sp)
                    Text(text = "• Backup Format: Authenticated Encrypted JSON (v2)", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "• Security: Zero cloud telemetry, no analytics, 100% offline data integrity.", color = StatusCompleted, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Export Password Prompt
    pendingExportUri?.let { uri ->
        PasswordPromptDialog(
            title = "Encrypt Backup",
            message = "Enter a password to encrypt this backup with AES-256-GCM. You will need this password to restore your data.",
            confirmLabel = "Encrypt & Save",
            onDismiss = { pendingExportUri = null },
            onConfirm = { pwd ->
                viewModel.exportEncryptedBackup(context, uri, pwd)
                pendingExportUri = null
            }
        )
    }

    // Restore Password Prompt
    pendingImportUri?.let { uri ->
        PasswordPromptDialog(
            title = "Decrypt & Restore",
            message = "Enter the password used when creating this backup file to verify integrity and restore records.",
            confirmLabel = "Decrypt & Restore",
            onDismiss = { pendingImportUri = null },
            onConfirm = { pwd ->
                viewModel.restoreEncryptedBackup(context, uri, pwd)
                pendingImportUri = null
            }
        )
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Set Application PIN", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to lock Property CRM:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pinError!!, color = StatusOverdue, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits."
                        } else if (pinInput != confirmPinInput) {
                            pinError = "PINs do not match."
                        } else {
                            viewModel.setPin(pinInput)
                            showSetPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Set PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Battery Troubleshooting Dialog (Item 11)
    if (showBatteryGuideDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = StatusUpcoming)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xiaomi / MIUI Background Guide", color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "To guarantee 9:00 AM follow-up reminders on Xiaomi / Redmi / HyperOS devices:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "1. Enable 'Autostart' in App Info for Property CRM.", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "2. Set Battery Saver to 'No restrictions'.", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "3. Lock Property CRM in Recent Apps tray.", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "4. Note: If an app is force-stopped by Android OS, alarms cannot trigger until reopened.", color = StatusOverdue, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Text("Open App Settings (Autostart)", color = PrimaryBlue, fontSize = 12.sp)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("Ignore Battery Optimization", color = PrimaryBlue, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBatteryGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Import Result Dialog
    importResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { importResultDialog = null },
            title = {
                Text(
                    text = if (result.success) "Restore Successful" else "Restore Failed",
                    color = if (result.success) StatusCompleted else StatusOverdue
                )
            },
            text = {
                if (result.success) {
                    Column {
                        Text(text = "Database restoration completed successfully:", color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• Customers Restored: ${result.customersImported}", color = TextSecondary)
                        Text(text = "• Follow-ups Restored: ${result.followUpsImported}", color = TextSecondary)
                        Text(text = "• Site Visits Restored: ${result.siteVisitsImported}", color = TextSecondary)
                        Text(text = "• Timeline Interactions: ${result.interactionsImported}", color = TextSecondary)
                        if (result.customersSkipped > 0) {
                            Text(text = "• Existing Customers Merged: ${result.customersSkipped}", color = TextMuted)
                        }
                    }
                } else {
                    Text(text = result.errorMessage ?: "Failed to decrypt or read backup payload.", color = StatusOverdue)
                }
            },
            confirmButton = {
                Button(
                    onClick = { importResultDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
