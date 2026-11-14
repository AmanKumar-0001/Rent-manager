package com.tenantmanager

import android.app.Application
import com.tenantmanager.di.AppContainer
import com.tenantmanager.di.DefaultAppContainer
import com.tenantmanager.notification.NotificationHelper
import com.tenantmanager.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TenantApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        NotificationHelper.createNotificationChannels(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminderHour = container.settingsRepository.getSettings().first().reminderHour
                ReminderScheduler.scheduleDailyReminder(this@TenantApplication, reminderHour)
            } catch (_: Exception) {
            }
        }
    }
}
