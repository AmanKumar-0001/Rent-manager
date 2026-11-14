package com.tenantmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val GRACE_DAYS = intPreferencesKey("grace_days")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences ->
            AppSettings(
                currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "₹",
                graceDays = preferences[PreferencesKeys.GRACE_DAYS] ?: 5,
                reminderHour = preferences[PreferencesKeys.REMINDER_HOUR] ?: 9,
                appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
            )
        }
    }

    override suspend fun updateCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    override suspend fun updateGraceDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRACE_DAYS] = days
        }
    }

    override suspend fun updateReminderHour(hour: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_HOUR] = hour
        }
    }

    override suspend fun updateAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }
}

