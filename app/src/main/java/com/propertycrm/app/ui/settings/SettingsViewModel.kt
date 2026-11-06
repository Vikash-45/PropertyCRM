package com.propertycrm.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.model.BackupData
import com.propertycrm.app.data.model.ImportResult
import com.propertycrm.app.data.repository.BackupRepository
import com.propertycrm.app.notification.ReminderNotificationManager
import com.propertycrm.app.notification.ReminderScheduler
import com.propertycrm.app.util.SecurityUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isReminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val appLanguage: String = "system",
    val isExactAlarmPermitted: Boolean = true,
    val isPinLockEnabled: Boolean = false,
    val lastBackupTime: Long? = null,
    val lastRestoreTime: Long? = null,
    val isOperationRunning: Boolean = false
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ShowImportResult(val result: ImportResult) : SettingsEvent()
}

class SettingsViewModel(
    private val preferenceManager: PreferenceManager,
    private val backupRepository: BackupRepository,
    private val reminderScheduler: ReminderScheduler,
    private val reminderNotificationManager: ReminderNotificationManager
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow: SharedFlow<SettingsEvent> = _eventFlow.asSharedFlow()

    private val _isOperationRunning = MutableStateFlow(false)

    private data class PrefGroup1(val enabled: Boolean, val hour: Int, val min: Int, val lang: String)
    private data class PrefGroup2(val pinLock: Boolean, val lastBackup: Long?, val lastRestore: Long?, val opRunning: Boolean)

    private val prefFlow1 = combine(
        preferenceManager.isReminderEnabled,
        preferenceManager.reminderHour,
        preferenceManager.reminderMinute,
        preferenceManager.appLanguage
    ) { enabled, hour, min, lang -> PrefGroup1(enabled, hour, min, lang) }

    private val prefFlow2 = combine(
        preferenceManager.isPinLockEnabled,
        preferenceManager.lastBackupTime,
        preferenceManager.lastRestoreTime,
        _isOperationRunning
    ) { pinLock, lastBackup, lastRestore, opRunning -> PrefGroup2(pinLock, lastBackup, lastRestore, opRunning) }

    val uiState: StateFlow<SettingsUiState> = combine(
        prefFlow1,
        prefFlow2
    ) { p1, p2 ->
        SettingsUiState(
            isReminderEnabled = p1.enabled,
            reminderHour = p1.hour,
            reminderMinute = p1.min,
            appLanguage = p1.lang,
            isExactAlarmPermitted = reminderScheduler.canScheduleExactAlarms(),
            isPinLockEnabled = p2.pinLock,
            lastBackupTime = p2.lastBackup,
            lastRestoreTime = p2.lastRestore,
            isOperationRunning = p2.opRunning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleNextDailyReminder()
            } else {
                reminderScheduler.cancelReminder()
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferenceManager.setReminderTime(hour, minute)
            reminderScheduler.scheduleNextDailyReminder()
            _eventFlow.emit(SettingsEvent.ShowToast("Reminder time updated to %02d:%02d".format(hour, minute)))
        }
    }

    fun triggerTestNotification() {
        viewModelScope.launch {
            reminderNotificationManager.checkAndShowDailyReminderNotification(forceShowTest = true)
            _eventFlow.emit(SettingsEvent.ShowToast("Test notification sent! Check notification shade."))
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPin(pin, salt)
            preferenceManager.setPinLock(enabled = true, hash = hash, salt = salt)
            _eventFlow.emit(SettingsEvent.ShowToast("Application PIN lock enabled successfully."))
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            preferenceManager.setPinLock(enabled = false)
            _eventFlow.emit(SettingsEvent.ShowToast("Application PIN lock disabled."))
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupRepository.exportCustomersToCsv(outputStream)
                    if (result.isSuccess) {
                        _eventFlow.emit(SettingsEvent.ShowToast("Exported ${result.getOrNull()} customers to CSV."))
                    } else {
                        _eventFlow.emit(SettingsEvent.ShowToast("CSV export failed: ${result.exceptionOrNull()?.message}"))
                    }
                }
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.ShowToast("Export error: ${e.message}"))
            } finally {
                _isOperationRunning.value = false
            }
        }
    }

    fun exportEncryptedBackup(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupRepository.exportEncryptedBackup(password, outputStream)
                    if (result.isSuccess) {
                        preferenceManager.updateLastBackupTime()
                        _eventFlow.emit(SettingsEvent.ShowToast("AES-256 encrypted backup saved successfully!"))
                    } else {
                        _eventFlow.emit(SettingsEvent.ShowToast("Backup failed: ${result.exceptionOrNull()?.message}"))
                    }
                }
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.ShowToast("Backup error: ${e.message}"))
            } finally {
                _isOperationRunning.value = false
            }
        }
    }

    fun restoreEncryptedBackup(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val result = backupRepository.restoreEncryptedBackup(inputStream, password)
                    if (result.success) {
                        preferenceManager.updateLastRestoreTime()
                    }
                    _eventFlow.emit(SettingsEvent.ShowImportResult(result))
                    reminderScheduler.scheduleNextDailyReminder()
                }
            } catch (e: Exception) {
                _eventFlow.emit(SettingsEvent.ShowToast("Restore error: ${e.message}"))
            } finally {
                _isOperationRunning.value = false
            }
        }
    }
}
