package com.tenantmanager.domain.calculator

import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.PortfolioSummary
import com.tenantmanager.domain.model.RentalPeriod
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import java.time.LocalDate
import java.time.YearMonth

object RentCalculator {

    fun calculateLedger(
        tenant: Tenant,
        payments: List<Payment>,
        asOfDate: LocalDate,
        graceDays: Int = 5,
        proratedAmounts: Map<YearMonth, Money> = emptyMap()
    ): TenantLedger {
        val months = PeriodGenerator.generatePeriodMonths(tenant.joinDate, tenant.leaveDate, asOfDate)

        val paymentsByMonth = payments.filter { it.tenantId == tenant.id }
            .groupBy { it.periodMonth }
            .mapValues { (_, pList) ->
                pList.fold(Money.ZERO) { acc, p -> acc + p.amount }
            }

        var carriedCredit = Money.ZERO
        val rentalPeriods = mutableListOf<RentalPeriod>()
        var totalPaidAcrossPeriods = Money.ZERO
        var totalBalanceAcrossPeriods = Money.ZERO

        val firstMonth = YearMonth.from(tenant.joinDate)

        for (month in months) {
            val standardDueDate = PeriodGenerator.resolveDueDate(month, tenant.dueDay)
            val dueDate = if (month == firstMonth && standardDueDate.isBefore(tenant.joinDate)) {
                tenant.joinDate
            } else {
                standardDueDate
            }

            val expectedRent = proratedAmounts[month] ?: tenant.monthlyRent
            val directPayments = paymentsByMonth[month] ?: Money.ZERO
            val creditApplied = carriedCredit
            val totalAvailable = directPayments + creditApplied

            val satisfied = if (totalAvailable >= expectedRent) expectedRent else totalAvailable
            val balance = expectedRent - satisfied
            carriedCredit = totalAvailable - satisfied

            val status = when {
                balance.isZero -> PeriodStatus.PAID
                satisfied.isPositive -> PeriodStatus.PARTIALLY_PAID
                else -> PeriodStatus.UNPAID
            }

            totalPaidAcrossPeriods += directPayments
            totalBalanceAcrossPeriods += balance

            rentalPeriods.add(
                RentalPeriod(
                    periodMonth = month,
                    dueDate = dueDate,
                    expectedRent = expectedRent,
                    directPaymentsTotal = directPayments,
                    creditApplied = creditApplied,
                    totalPaid = satisfied,
                    balance = balance,
                    status = status
                )
            )
        }

        val status = resolveTenantStatus(tenant, rentalPeriods, asOfDate, graceDays)
        val nextPaymentDate = calculateNextPaymentDate(tenant, rentalPeriods, asOfDate)

        return TenantLedger(
            tenant = tenant,
            periods = rentalPeriods,
            totalPaid = totalPaidAcrossPeriods,
            totalBalance = totalBalanceAcrossPeriods,
            status = status,
            nextPaymentDate = nextPaymentDate
        )
    }

    fun resolveTenantStatus(
        tenant: Tenant,
        periods: List<RentalPeriod>,
        asOfDate: LocalDate,
        graceDays: Int = 5
    ): TenantStatus {
        if (tenant.isArchived || (tenant.leaveDate != null && tenant.leaveDate.isBefore(asOfDate))) {
            return TenantStatus.LEFT
        }

        val hasOverdue = periods.any { period ->
            period.status != PeriodStatus.PAID && asOfDate.isAfter(period.dueDate.plusDays(graceDays.toLong()))
        }
        if (hasOverdue) {
            return TenantStatus.OVERDUE
        }

        val currentMonth = YearMonth.from(asOfDate)
        val currentPeriod = periods.firstOrNull { it.periodMonth == currentMonth }
            ?: periods.firstOrNull { it.status != PeriodStatus.PAID }

        if (currentPeriod != null && currentPeriod.status != PeriodStatus.PAID) {
            val dueDate = currentPeriod.dueDate
            val windowStart = dueDate.minusDays(5)
            val windowEnd = dueDate.plusDays(graceDays.toLong())
            if (!asOfDate.isBefore(windowStart) && !asOfDate.isAfter(windowEnd)) {
                return TenantStatus.DUE_SOON
            }
        }

        if (currentPeriod != null && currentPeriod.status == PeriodStatus.PAID) {
            val allPastPaid = periods.filter { it.dueDate.isBefore(currentPeriod.dueDate) }
                .all { it.status == PeriodStatus.PAID }
            if (allPastPaid) {
                return TenantStatus.PAID
            }
        }

        return TenantStatus.UPCOMING
    }

    fun calculateNextPaymentDate(
        tenant: Tenant,
        periods: List<RentalPeriod>,
        asOfDate: LocalDate
    ): LocalDate? {
        if (tenant.isArchived || (tenant.leaveDate != null && tenant.leaveDate.isBefore(asOfDate))) {
            return null
        }

        val unpaidOnOrAfter = periods.filter { it.status != PeriodStatus.PAID && !it.dueDate.isBefore(asOfDate) }
        if (unpaidOnOrAfter.isNotEmpty()) {
            return unpaidOnOrAfter.first().dueDate
        }

        val anyUnpaid = periods.filter { it.status != PeriodStatus.PAID }
        if (anyUnpaid.isNotEmpty()) {
            return anyUnpaid.first().dueDate
        }

        val nextMonth = YearMonth.from(asOfDate).plusMonths(1)
        return PeriodGenerator.resolveDueDate(nextMonth, tenant.dueDay)
    }

    fun calculatePortfolioSummary(
        tenants: List<Tenant>,
        ledgers: List<TenantLedger>,
        asOfDate: LocalDate
    ): PortfolioSummary {
        val currentMonth = YearMonth.from(asOfDate)
        val activeTenants = tenants.filter { !it.isArchived && (it.leaveDate == null || !it.leaveDate.isBefore(asOfDate)) }
        val activeIds = activeTenants.map { it.id }.toSet()

        var expectedTotal = Money.ZERO
        var collectedTotal = Money.ZERO
        var overdueTotal = Money.ZERO

        for (ledger in ledgers) {
            if (ledger.tenant.id in activeIds) {
                val currentPeriod = ledger.periods.firstOrNull { it.periodMonth == currentMonth }
                if (currentPeriod != null) {
                    expectedTotal += currentPeriod.expectedRent
                    collectedTotal += currentPeriod.totalPaid
                }
                overdueTotal += ledger.totalBalance
            }
        }

        return PortfolioSummary(
            activeTenantsCount = activeTenants.size,
            totalExpectedCurrentMonth = expectedTotal,
            totalCollectedCurrentMonth = collectedTotal,
            totalOverdue = overdueTotal
        )
    }
}

