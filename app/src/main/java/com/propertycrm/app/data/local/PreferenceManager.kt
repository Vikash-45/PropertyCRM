package com.propertycrm.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "property_crm_preferences")

class PreferenceManager(private val context: Context) {

    companion object {
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        // App Security Lock (PIN)
        val KEY_PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")

        // Backup audit metadata
        val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val KEY_LAST_RESTORE_TIME = longPreferencesKey("last_restore_time")

        // AI Custom Gateway & Model Configuration
        val KEY_AI_GATEWAY_URL = stringPreferencesKey("ai_gateway_url")
        val KEY_AI_API_KEY = stringPreferencesKey("ai_api_key")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val KEY_AI_TEMPERATURE = floatPreferencesKey("ai_temperature")

        // Call Recording & Audio Summarization
        val KEY_AUTO_RECORD_CALLS = booleanPreferencesKey("auto_record_calls")
        val KEY_AUTO_SUMMARIZE_CALLS = booleanPreferencesKey("auto_summarize_calls")
        val KEY_DEFAULT_VISIT_LEAD_TIME = intPreferencesKey("default_visit_lead_time")
    }

    val aiGatewayUrl: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AI_GATEWAY_URL] ?: "https://api.openai.com/v1" }

    val aiApiKey: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AI_API_KEY] ?: "" }

    val aiModel: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AI_MODEL] ?: "gpt-4o-mini" }

    val aiSystemPrompt: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            it[KEY_AI_SYSTEM_PROMPT] ?: "You are an expert real estate AI copilot for Property CRM. You have full access to manage CRM customer records, site visits, and follow-ups through tools. Always be concise, helpful, and take action when requested."
        }

    val aiTemperature: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AI_TEMPERATURE] ?: 0.7f }

    val autoRecordCalls: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTO_RECORD_CALLS] ?: true }

    val autoSummarizeCalls: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTO_SUMMARIZE_CALLS] ?: true }

    val defaultVisitLeadTime: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DEFAULT_VISIT_LEAD_TIME] ?: 60 }

    val isReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REMINDER_ENABLED] ?: true
        }

    val reminderHour: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REMINDER_HOUR] ?: 9 // Default 9:00 AM
        }

    val reminderMinute: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_REMINDER_MINUTE] ?: 0 // Default :00
        }

    val appLanguage: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_APP_LANGUAGE] ?: "system"
        }

    val isPinLockEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_PIN_LOCK_ENABLED] ?: false
        }

    val pinHash: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_PIN_HASH]
        }

    val pinSalt: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_PIN_SALT]
        }

    val lastBackupTime: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_LAST_BACKUP_TIME]
        }

    val lastRestoreTime: Flow<Long?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_LAST_RESTORE_TIME]
        }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setPinLock(enabled: Boolean, hash: String? = null, salt: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_LOCK_ENABLED] = enabled
            if (hash != null) preferences[KEY_PIN_HASH] = hash else if (!enabled) preferences.remove(KEY_PIN_HASH)
            if (salt != null) preferences[KEY_PIN_SALT] = salt else if (!enabled) preferences.remove(KEY_PIN_SALT)
        }
    }

    suspend fun updateLastBackupTime(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] = timestamp
        }
    }

    suspend fun updateLastRestoreTime(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_RESTORE_TIME] = timestamp
        }
    }

    suspend fun setAiGatewayConfig(
        gatewayUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String? = null,
        temperature: Float? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_GATEWAY_URL] = gatewayUrl.trim().trimEnd('/')
            preferences[KEY_AI_API_KEY] = apiKey.trim()
            preferences[KEY_AI_MODEL] = model.trim()
            if (systemPrompt != null) preferences[KEY_AI_SYSTEM_PROMPT] = systemPrompt
            if (temperature != null) preferences[KEY_AI_TEMPERATURE] = temperature
        }
    }

    suspend fun setAutoRecordCalls(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_RECORD_CALLS] = enabled
        }
    }

    suspend fun setAutoSummarizeCalls(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SUMMARIZE_CALLS] = enabled
        }
    }

    suspend fun setDefaultVisitLeadTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VISIT_LEAD_TIME] = minutes
        }
    }
}
