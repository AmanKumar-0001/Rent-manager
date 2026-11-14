package com.tenantmanager.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tenantmanager.TenantApplication
import com.tenantmanager.data.local.entity.AlertHistoryEntity
import com.tenantmanager.di.AppContainer
import com.tenantmanager.di.DefaultAppContainer
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.TenantStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class DailyNotificationWorker(
    context: Context,
    params: WorkerParameters,
    private val customContainer: AppContainer? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container: AppContainer = customContainer
            ?: (applicationContext as? TenantApplication)?.container
            ?: DefaultAppContainer(applicationContext)

        val settings = container.settingsRepository.getSettings().first()
        val tenants = container.tenantRepository.getActiveTenants().first()
        val allPayments = container.paymentRepository.getAllPayments().first()
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM uuuu")

        for (tenant in tenants) {
            val tenantPayments = allPayments.filter { it.tenantId == tenant.id }
            val ledger = RentCalculator.calculateLedger(
                tenant = tenant,
                payments = tenantPayments,
                asOfDate = today,
                graceDays = settings.graceDays
            )

            when (ledger.status) {
                TenantStatus.OVERDUE -> {
                    val overduePeriod = ledger.periods.firstOrNull {
                        (it.status == PeriodStatus.UNPAID || it.status == PeriodStatus.PARTIALLY_PAID) &&
                            today.isAfter(it.dueDate.plusDays(settings.graceDays.toLong()))
                    } ?: ledger.periods.firstOrNull { it.status == PeriodStatus.UNPAID || it.status == PeriodStatus.PARTIALLY_PAID }

                    if (overduePeriod != null) {
                        val periodMonthStr = overduePeriod.periodMonth.toString()
                        val alertType = "OVERDUE"
                        val alreadySent = container.database.alertHistoryDao().hasAlertBeenSent(
                            tenantId = tenant.id,
                            periodMonth = periodMonthStr,
                            alertType = alertType
                        )

                        if (!alreadySent) {
                            val formattedDueDate = overduePeriod.dueDate.format(dateFormatter)
                            val title = "Rent Overdue: ${tenant.name}"
                            val message = "Rent of ${tenant.monthlyRent.formatINR(settings.currencySymbol)} for unit ${tenant.unitName} was due on $formattedDueDate."

                            NotificationHelper.showNotification(
                                context = applicationContext,
                                notificationId = tenant.id.hashCode() xor 0x1000,
                                title = title,
                                message = message
                            )

                            container.database.alertHistoryDao().recordAlert(
                                AlertHistoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    tenantId = tenant.id,
                                    periodMonth = periodMonthStr,
                                    alertType = alertType
                                )
                            )
                        }
                    }
                }

                TenantStatus.DUE_SOON -> {
                    val dueSoonPeriod = ledger.periods.firstOrNull {
                        (it.status == PeriodStatus.UNPAID || it.status == PeriodStatus.PARTIALLY_PAID) &&
                            !today.isAfter(it.dueDate) &&
                            !today.isBefore(it.dueDate.minusDays(5))
                    } ?: ledger.periods.firstOrNull { it.status == PeriodStatus.UNPAID || it.status == PeriodStatus.PARTIALLY_PAID }

                    if (dueSoonPeriod != null) {
                        val periodMonthStr = dueSoonPeriod.periodMonth.toString()
                        val alertType = "DUE_SOON"
                        val alreadySent = container.database.alertHistoryDao().hasAlertBeenSent(
                            tenantId = tenant.id,
                            periodMonth = periodMonthStr,
                            alertType = alertType
                        )

                        if (!alreadySent) {
                            val formattedDueDate = dueSoonPeriod.dueDate.format(dateFormatter)
                            val title = "Rent Due Soon: ${tenant.name}"
                            val message = "Rent of ${tenant.monthlyRent.formatINR(settings.currencySymbol)} for unit ${tenant.unitName} is due on $formattedDueDate."

                            NotificationHelper.showNotification(
                                context = applicationContext,
                                notificationId = tenant.id.hashCode() xor 0x2000,
                                title = title,
                                message = message
                            )

                            container.database.alertHistoryDao().recordAlert(
                                AlertHistoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    tenantId = tenant.id,
                                    periodMonth = periodMonthStr,
                                    alertType = alertType
                                )
                            )
                        }
                    }
                }

                else -> {
                    // PAID, UPCOMING, LEFT - no alerts needed
                }
            }
        }

        return Result.success()
    }
}

