package com.tenantmanager.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val WORK_NAME_DAILY_REMINDER = "daily_rent_reminder"

    fun scheduleDailyReminder(context: Context, reminderHour: Int = 9) {
        val now = LocalTime.now()
        val target = LocalTime.of(reminderHour.coerceIn(0, 23), 0)

        val initialDelayMinutes = if (now.isBefore(target)) {
            Duration.between(now, target).toMinutes()
        } else {
            Duration.between(now, target.plusHours(24)).toMinutes()
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun triggerImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyNotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

