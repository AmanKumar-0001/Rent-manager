package com.tenantmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tenantmanager.TenantApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            val application = context.applicationContext as? TenantApplication

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminderHour = application?.container?.settingsRepository
                        ?.getSettings()
                        ?.first()
                        ?.reminderHour ?: 9

                    ReminderScheduler.scheduleDailyReminder(context, reminderHour)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}

