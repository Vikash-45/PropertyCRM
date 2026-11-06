package com.propertycrm.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.data.local.PreferenceManager
import com.propertycrm.app.data.model.ImportResult
import com.propertycrm.app.data.repository.BackupRepository
import com.propertycrm.app.notification.ReminderNotificationManager
import com.propertycrm.app.notification.ReminderScheduler
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
    val isExporting: Boolean = false,
    val isImporting: Boolean = false
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

    val uiState: StateFlow<SettingsUiState> = combine(
        preferenceManager.isReminderEnabled,
        preferenceManager.reminderHour,
        preferenceManager.reminderMinute,
        preferenceManager.appLanguage,
        _isOperationRunning
    ) { enabled, hour, min, lang, opRunning ->
        SettingsUiState(
            isReminderEnabled = enabled,
            reminderHour = hour,
            reminderMinute = min,
            appLanguage = lang,
            isExactAlarmPermitted = reminderScheduler.canScheduleExactAlarms(),
            isExporting = opRunning,
            isImporting = opRunning
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
            _eventFlow.emit(SettingsEvent.ShowToast("Test notification sent! Check your notification bar."))
        }
    }

    fun exportCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupRepository.exportCustomersToCsv(outputStream)
                    if (result.isSuccess) {
                        _eventFlow.emit(SettingsEvent.ShowToast("Successfully exported ${result.getOrNull()} customers to CSV!"))
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

    fun exportJsonBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupRepository.exportCompleteBackupToJson(outputStream)
                    if (result.isSuccess) {
                        _eventFlow.emit(SettingsEvent.ShowToast("Complete JSON backup created successfully!"))
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

    fun importJsonBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isOperationRunning.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val result = backupRepository.importBackupFromJson(inputStream)
                    _eventFlow.emit(SettingsEvent.ShowImportResult(result))
                    // Re-evaluate notification reminders for newly restored follow-ups
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
