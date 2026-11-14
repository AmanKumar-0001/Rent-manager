package com.tenantmanager.domain.repository

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val currencySymbol: String = "₹",
    val graceDays: Int = 5,
    val reminderHour: Int = 9,
    val appLockEnabled: Boolean = false
)

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateCurrencySymbol(symbol: String)
    suspend fun updateGraceDays(days: Int)
    suspend fun updateReminderHour(hour: Int)
    suspend fun updateAppLockEnabled(enabled: Boolean)
}

