package com.tenantmanager.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantStatus
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RentCalculatorTest {

    private fun createTenant(
        id: String = "t1",
        joinDate: LocalDate = LocalDate.of(2026, 1, 1),
        leaveDate: LocalDate? = null,
        monthlyRent: Money = Money.ofRupees(10000),
        dueDay: Int = 5,
        isArchived: Boolean = false
    ): Tenant {
        return Tenant(
            id = id,
            propertyId = "p1",
            name = "Test Tenant",
            phone = "9876543210",
            email = "test@example.com",
            unitName = "A-101",
            joinDate = joinDate,
            leaveDate = leaveDate,
            monthlyRent = monthlyRent,
            dueDay = dueDay,
            isArchived = isArchived
        )
    }

    private fun createPayment(
        id: String = "pay1",
        tenantId: String = "t1",
        amount: Money = Money.ofRupees(10000),
        paidDate: LocalDate = LocalDate.of(2026, 1, 5),
        periodMonth: YearMonth = YearMonth.of(2026, 1)
    ): Payment {
        return Payment(
            id = id,
            tenantId = tenantId,
            amount = amount,
            paidDate = paidDate,
            periodMonth = periodMonth
        )
    }

    @Test
    fun testMidMonthJoinDate_firstPeriodDueOnJoinDateIfDueDayPassed() {
        // TS-04: Joining mid-month (March 15, dueDay = 5). Standard due date (March 5) is before join date.
        // First period is March, due on March 15.
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 3, 15),
            dueDay = 5
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 3, 20)
        )

        assertThat(ledger.periods).hasSize(1)
        val firstPeriod = ledger.periods.first()
        assertThat(firstPeriod.periodMonth).isEqualTo(YearMonth.of(2026, 3))
        assertThat(firstPeriod.dueDate).isEqualTo(LocalDate.of(2026, 3, 15))
        assertThat(firstPeriod.expectedRent).isEqualTo(Money.ofRupees(10000))
        assertThat(firstPeriod.balance).isEqualTo(Money.ofRupees(10000))
        assertThat(firstPeriod.status).isEqualTo(PeriodStatus.UNPAID)
    }

    @Test
    fun testMidMonthJoinDate_withManualProration() {
        // Rule 6: manual proration
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 3, 15),
            dueDay = 5
        )
        val prorations = mapOf(YearMonth.of(2026, 3) to Money.ofRupees(5000))
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 3, 20),
            proratedAmounts = prorations
        )

        val firstPeriod = ledger.periods.first()
        assertThat(firstPeriod.expectedRent).isEqualTo(Money.ofRupees(5000))
        assertThat(firstPeriod.balance).isEqualTo(Money.ofRupees(5000))
    }

    @Test
    fun testPartialPaymentInPeriod() {
        // TS-05: Partial payment: Rent ₹10,000, paid ₹4,000
        val tenant = createTenant()
        val payment = createPayment(
            amount = Money.ofRupees(4000),
            periodMonth = YearMonth.of(2026, 1)
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = listOf(payment),
            asOfDate = LocalDate.of(2026, 1, 10)
        )

        val period = ledger.periods.first()
        assertThat(period.totalPaid).isEqualTo(Money.ofRupees(4000))
        assertThat(period.balance).isEqualTo(Money.ofRupees(6000))
        assertThat(period.status).isEqualTo(PeriodStatus.PARTIALLY_PAID)
    }

    @Test
    fun testExactPaymentInPeriod() {
        // TS-06: Exact payment: Rent ₹10,000, paid ₹10,000
        val tenant = createTenant()
        val payment = createPayment(
            amount = Money.ofRupees(10000),
            periodMonth = YearMonth.of(2026, 1)
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = listOf(payment),
            asOfDate = LocalDate.of(2026, 1, 10)
        )

        val period = ledger.periods.first()
        assertThat(period.totalPaid).isEqualTo(Money.ofRupees(10000))
        assertThat(period.balance).isEqualTo(Money.ZERO)
        assertThat(period.status).isEqualTo(PeriodStatus.PAID)
    }

    @Test
    fun testOverpaymentCarriedForwardAcrossMultiplePeriods() {
        // TS-07: Overpayment carry-forward: Rent ₹10,000/mo. Paid ₹25,000 in P1.
        // P1 = PAID (credit ₹15k), P2 = PAID (credit ₹5k), P3 = PARTIALLY_PAID (credit applied ₹5k, bal ₹5k)
        val tenant = createTenant(joinDate = LocalDate.of(2026, 1, 1))
        val payment = createPayment(
            amount = Money.ofRupees(25000),
            periodMonth = YearMonth.of(2026, 1)
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = listOf(payment),
            asOfDate = LocalDate.of(2026, 3, 10)
        )

        assertThat(ledger.periods).hasSize(3)
        val p1 = ledger.periods[0]
        val p2 = ledger.periods[1]
        val p3 = ledger.periods[2]

        assertThat(p1.status).isEqualTo(PeriodStatus.PAID)
        assertThat(p1.balance).isEqualTo(Money.ZERO)

        assertThat(p2.status).isEqualTo(PeriodStatus.PAID)
        assertThat(p2.creditApplied).isEqualTo(Money.ofRupees(15000))
        assertThat(p2.balance).isEqualTo(Money.ZERO)

        assertThat(p3.status).isEqualTo(PeriodStatus.PARTIALLY_PAID)
        assertThat(p3.creditApplied).isEqualTo(Money.ofRupees(5000))
        assertThat(p3.balance).isEqualTo(Money.ofRupees(5000))
    }

    @Test
    fun testGracePeriodBoundary_onTheDay() {
        // TS-08: Grace period boundary (on the day).
        // Due Nov 5, grace 5, today Nov 10 (dueDate + graceDays == today) -> DUE_SOON
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 11, 1),
            dueDay = 5
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 10),
            graceDays = 5
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.DUE_SOON)
    }

    @Test
    fun testGracePeriodBoundary_oneDayAfter() {
        // TS-09: Grace period boundary (one day after).
        // Due Nov 5, grace 5, today Nov 11 (dueDate + graceDays < today) -> OVERDUE
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 11, 1),
            dueDay = 5
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 11),
            graceDays = 5
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.OVERDUE)
    }

    @Test
    fun testLeftTenant_statusAndNextPaymentDate() {
        // TS-10: Left tenant: leaveDate set and before today -> Status LEFT, nextPaymentDate = null
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 1, 1),
            leaveDate = LocalDate.of(2026, 10, 31)
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 14)
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.LEFT)
        assertThat(ledger.nextPaymentDate).isNull()
    }

    @Test
    fun testArchivedTenant_statusAndNextPaymentDate() {
        val tenant = createTenant(isArchived = true)
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 14)
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.LEFT)
        assertThat(ledger.nextPaymentDate).isNull()
    }

    @Test
    fun testAllPeriodsPaid_nextMonthDueDate() {
        // TS-11: All periods paid up to date -> nextPaymentDate = next month's due date (Dec 5)
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 11, 1),
            dueDay = 5
        )
        val payment = createPayment(
            amount = Money.ofRupees(10000),
            periodMonth = YearMonth.of(2026, 11)
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = listOf(payment),
            asOfDate = LocalDate.of(2026, 11, 14)
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.PAID)
        assertThat(ledger.nextPaymentDate).isEqualTo(LocalDate.of(2026, 12, 5))
    }

    @Test
    fun testTenantJoiningSameMonthAsToday() {
        // TS-12: Tenant joining in the same month as today (Joined Nov 10, dueDay = 15, today Nov 14)
        // Period 2026-11, status DUE_SOON, next date Nov 15
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 11, 10),
            dueDay = 15
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 14),
            graceDays = 5
        )

        assertThat(ledger.periods).hasSize(1)
        assertThat(ledger.periods.first().dueDate).isEqualTo(LocalDate.of(2026, 11, 15))
        assertThat(ledger.status).isEqualTo(TenantStatus.DUE_SOON)
        assertThat(ledger.nextPaymentDate).isEqualTo(LocalDate.of(2026, 11, 15))
    }

    @Test
    fun testStatusUpcoming() {
        // Active tenant, current period due Nov 25, today is Nov 10 (>5 days before Nov 25) -> UPCOMING
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 11, 1),
            dueDay = 25
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 10),
            graceDays = 5
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.UPCOMING)
    }

    @Test
    fun testArchivedTenantExcludedFromPortfolioTotals() {
        // TS-13: Archived tenant excluded from active counts and expected income
        val activeTenant1 = createTenant(id = "t1", monthlyRent = Money.ofRupees(10000))
        val activeTenant2 = createTenant(id = "t2", monthlyRent = Money.ofRupees(15000))
        val archivedTenant = createTenant(id = "t3", monthlyRent = Money.ofRupees(20000), isArchived = true)

        val asOf = LocalDate.of(2026, 11, 14)
        val l1 = RentCalculator.calculateLedger(activeTenant1, emptyList(), asOf)
        val l2 = RentCalculator.calculateLedger(activeTenant2, emptyList(), asOf)
        val l3 = RentCalculator.calculateLedger(archivedTenant, emptyList(), asOf)

        val summary = RentCalculator.calculatePortfolioSummary(
            tenants = listOf(activeTenant1, activeTenant2, archivedTenant),
            ledgers = listOf(l1, l2, l3),
            asOfDate = asOf
        )

        assertThat(summary.activeTenantsCount).isEqualTo(2)
        assertThat(summary.totalExpectedCurrentMonth).isEqualTo(Money.ofRupees(25000))
        assertThat(summary.totalCollectedCurrentMonth).isEqualTo(Money.ZERO)
        assertThat(summary.totalOverdue).isEqualTo(l1.totalBalance + l2.totalBalance)
    }

    @Test
    fun testNextPaymentDateWhenPastOverdueExists() {
        // Past unpaid period exists before today -> nextPaymentDate returns that overdue due date
        val tenant = createTenant(
            joinDate = LocalDate.of(2026, 9, 1),
            dueDay = 5
        )
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 11, 14)
        )

        assertThat(ledger.status).isEqualTo(TenantStatus.OVERDUE)
        assertThat(ledger.nextPaymentDate).isEqualTo(LocalDate.of(2026, 9, 5))
    }
}

