package com.tenantmanager.domain

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.calculator.PeriodGenerator
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.PortfolioSummary
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.RentalPeriod
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.domain.repository.AppSettings
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DomainCoverageTest {

    @Test
    fun testDataModelsFullCoverage() {
        // Property
        val prop1 = Property(id = "p1", name = "Villa 1", address = "Street 1", defaultRent = Money.ofRupees(15000))
        val prop2 = prop1.copy(address = "Street 2")
        assertThat(prop1.id).isEqualTo("p1")
        assertThat(prop1.name).isEqualTo("Villa 1")
        assertThat(prop1.address).isEqualTo("Street 1")
        assertThat(prop1.defaultRent).isEqualTo(Money.ofRupees(15000))
        assertThat(prop1).isEqualTo(prop1)
        assertThat(prop1).isNotEqualTo(prop2)
        assertThat(prop1.hashCode()).isNotEqualTo(0)
        assertThat(prop1.toString()).contains("Villa 1")
        val propDefault = Property(id = "p2", name = "Villa 2")
        assertThat(propDefault.address).isNull()
        assertThat(propDefault.defaultRent).isEqualTo(Money.ZERO)

        // Tenant
        val t1 = Tenant(
            id = "t1",
            propertyId = "p1",
            name = "John Doe",
            phone = "1234567890",
            email = "john@example.com",
            unitName = "Unit 1",
            joinDate = LocalDate.of(2026, 1, 1),
            leaveDate = LocalDate.of(2026, 12, 31),
            monthlyRent = Money.ofRupees(10000),
            dueDay = 5,
            securityDeposit = Money.ofRupees(20000),
            depositReturnedDate = LocalDate.of(2027, 1, 5),
            notes = "Good tenant",
            isArchived = false,
            lastModified = 12345L
        )
        val t2 = t1.copy(name = "Jane Doe")
        assertThat(t1.propertyId).isEqualTo("p1")
        assertThat(t1.email).isEqualTo("john@example.com")
        assertThat(t1.securityDeposit).isEqualTo(Money.ofRupees(20000))
        assertThat(t1.depositReturnedDate).isEqualTo(LocalDate.of(2027, 1, 5))
        assertThat(t1.notes).isEqualTo("Good tenant")
        assertThat(t1.lastModified).isEqualTo(12345L)
        assertThat(t1).isEqualTo(t1)
        assertThat(t1).isNotEqualTo(t2)
        assertThat(t1.hashCode()).isNotEqualTo(0)
        assertThat(t1.toString()).contains("John Doe")

        // Default tenant constructor args
        val tDefault = Tenant(
            id = "tDefault",
            name = "Def",
            phone = "123",
            unitName = "U",
            joinDate = LocalDate.of(2026, 1, 1),
            monthlyRent = Money.ofRupees(5000),
            dueDay = 1
        )
        assertThat(tDefault.propertyId).isNull()
        assertThat(tDefault.email).isNull()
        assertThat(tDefault.leaveDate).isNull()
        assertThat(tDefault.securityDeposit).isEqualTo(Money.ZERO)
        assertThat(tDefault.depositReturnedDate).isNull()
        assertThat(tDefault.notes).isNull()
        assertThat(tDefault.isArchived).isFalse()
        assertThat(tDefault.lastModified).isEqualTo(0L)

        // Payment
        val pay1 = Payment(
            id = "pay1",
            tenantId = "t1",
            amount = Money.ofRupees(10000),
            paidDate = LocalDate.of(2026, 1, 5),
            periodMonth = YearMonth.of(2026, 1),
            notes = "UPI",
            lastModified = 67890L
        )
        val pay2 = pay1.copy(amount = Money.ofRupees(5000))
        assertThat(pay1.notes).isEqualTo("UPI")
        assertThat(pay1.lastModified).isEqualTo(67890L)
        assertThat(pay1).isEqualTo(pay1)
        assertThat(pay1).isNotEqualTo(pay2)
        assertThat(pay1.hashCode()).isNotEqualTo(0)
        assertThat(pay1.toString()).contains("pay1")
        val payDefault = Payment(
            id = "pay3",
            tenantId = "t1",
            amount = Money.ofRupees(10000),
            paidDate = LocalDate.of(2026, 1, 5),
            periodMonth = YearMonth.of(2026, 1)
        )
        assertThat(payDefault.notes).isNull()
        assertThat(payDefault.lastModified).isEqualTo(0L)

        // RentalPeriod
        val rp1 = RentalPeriod(
            periodMonth = YearMonth.of(2026, 1),
            dueDate = LocalDate.of(2026, 1, 5),
            expectedRent = Money.ofRupees(10000),
            directPaymentsTotal = Money.ofRupees(10000),
            creditApplied = Money.ZERO,
            totalPaid = Money.ofRupees(10000),
            balance = Money.ZERO,
            status = PeriodStatus.PAID
        )
        val rp2 = rp1.copy(balance = Money.ofRupees(1000))
        assertThat(rp1).isEqualTo(rp1)
        assertThat(rp1).isNotEqualTo(rp2)
        assertThat(rp1.hashCode()).isNotEqualTo(0)
        assertThat(rp1.toString()).contains("2026-01")

        // TenantLedger
        val ledger1 = TenantLedger(
            tenant = t1,
            periods = listOf(rp1),
            totalPaid = Money.ofRupees(10000),
            totalBalance = Money.ZERO,
            status = TenantStatus.PAID,
            nextPaymentDate = LocalDate.of(2026, 2, 5)
        )
        val ledger2 = ledger1.copy(status = TenantStatus.OVERDUE)
        assertThat(ledger1).isEqualTo(ledger1)
        assertThat(ledger1).isNotEqualTo(ledger2)
        assertThat(ledger1.hashCode()).isNotEqualTo(0)
        assertThat(ledger1.toString()).contains("PAID")

        // PortfolioSummary
        val summary1 = PortfolioSummary(
            activeTenantsCount = 1,
            totalExpectedCurrentMonth = Money.ofRupees(10000),
            totalCollectedCurrentMonth = Money.ofRupees(10000),
            totalOverdue = Money.ZERO
        )
        val summary2 = summary1.copy(activeTenantsCount = 2)
        assertThat(summary1).isEqualTo(summary1)
        assertThat(summary1).isNotEqualTo(summary2)
        assertThat(summary1.hashCode()).isNotEqualTo(0)
        assertThat(summary1.toString()).contains("10000")

        // AppSettings
        val settings = AppSettings(
            currencySymbol = "$",
            graceDays = 3,
            reminderHour = 10,
            appLockEnabled = true
        )
        val settingsDefault = AppSettings()
        assertThat(settings.currencySymbol).isEqualTo("$")
        assertThat(settings.graceDays).isEqualTo(3)
        assertThat(settings.reminderHour).isEqualTo(10)
        assertThat(settings.appLockEnabled).isTrue()
        assertThat(settingsDefault.currencySymbol).isEqualTo("₹")
        assertThat(settingsDefault.graceDays).isEqualTo(5)
        assertThat(settingsDefault.reminderHour).isEqualTo(9)
        assertThat(settingsDefault.appLockEnabled).isFalse()
        assertThat(settings).isNotEqualTo(settingsDefault)
        assertThat(settings.hashCode()).isNotEqualTo(0)
        assertThat(settings.toString()).contains("reminderHour=10")

        // Enums
        for (status in TenantStatus.values()) {
            assertThat(TenantStatus.valueOf(status.name)).isEqualTo(status)
        }
        for (status in PeriodStatus.values()) {
            assertThat(PeriodStatus.valueOf(status.name)).isEqualTo(status)
        }

        // Money compareTo equal and times
        assertThat(Money.ofRupees(100).compareTo(Money.ofRupees(100))).isEqualTo(0)
        assertThat((Money.ofRupees(10) * 5).minorUnits).isEqualTo(5000L)
        assertThat(Money.ZERO.isPositive).isFalse()
    }

    @Test
    fun testRentCalculatorEdgeBranches() {
        val tenant = Tenant(
            id = "t1",
            name = "Branch Test",
            phone = "123",
            unitName = "B1",
            joinDate = LocalDate.of(2026, 1, 1),
            monthlyRent = Money.ofRupees(10000),
            dueDay = 5
        )

        // 1. resolveTenantStatus with default graceDays
        val ledger = RentCalculator.calculateLedger(
            tenant = tenant,
            payments = emptyList(),
            asOfDate = LocalDate.of(2026, 1, 2)
        )
        val statusDefault = RentCalculator.resolveTenantStatus(
            tenant = tenant,
            periods = ledger.periods,
            asOfDate = LocalDate.of(2026, 1, 2)
        )
        assertThat(statusDefault).isEqualTo(TenantStatus.DUE_SOON)

        // 2. Current period paid, BUT past period was NOT paid -> should NOT return PAID
        val p1 = RentalPeriod(
            periodMonth = YearMonth.of(2026, 1),
            dueDate = LocalDate.of(2026, 1, 5),
            expectedRent = Money.ofRupees(10000),
            directPaymentsTotal = Money.ZERO,
            creditApplied = Money.ZERO,
            totalPaid = Money.ZERO,
            balance = Money.ofRupees(10000),
            status = PeriodStatus.UNPAID
        )
        val p2 = RentalPeriod(
            periodMonth = YearMonth.of(2026, 2),
            dueDate = LocalDate.of(2026, 2, 5),
            expectedRent = Money.ofRupees(10000),
            directPaymentsTotal = Money.ofRupees(10000),
            creditApplied = Money.ZERO,
            totalPaid = Money.ofRupees(10000),
            balance = Money.ZERO,
            status = PeriodStatus.PAID
        )
        val statusMixed = RentCalculator.resolveTenantStatus(
            tenant = tenant,
            periods = listOf(p1, p2),
            asOfDate = LocalDate.of(2026, 2, 10),
            graceDays = 100
        )
        assertThat(statusMixed).isEqualTo(TenantStatus.UPCOMING)

        // 3. Current period not in ledger, but periods has an unpaid period
        val statusFallbackUnpaid = RentCalculator.resolveTenantStatus(
            tenant = tenant,
            periods = listOf(p1), // 2026-01 only
            asOfDate = LocalDate.of(2026, 1, 3) // Within 5 days of Jan 5
        )
        assertThat(statusFallbackUnpaid).isEqualTo(TenantStatus.DUE_SOON)

        // 4. Current period not in ledger and no unpaid periods (all paid)
        val statusEmpty = RentCalculator.resolveTenantStatus(
            tenant = tenant,
            periods = emptyList(),
            asOfDate = LocalDate.of(2026, 5, 1)
        )
        assertThat(statusEmpty).isEqualTo(TenantStatus.UPCOMING)

        // 5. Tenant with future leave date (e.g. leaveDate = Dec 31, 2026, asOfDate = Nov 14, 2026)
        val tenantFutureLeave = tenant.copy(leaveDate = LocalDate.of(2026, 12, 31))
        val statusFutureLeave = RentCalculator.resolveTenantStatus(
            tenant = tenantFutureLeave,
            periods = ledger.periods,
            asOfDate = LocalDate.of(2026, 1, 2)
        )
        assertThat(statusFutureLeave).isEqualTo(TenantStatus.DUE_SOON)
        val nextDateFuture = RentCalculator.calculateNextPaymentDate(
            tenant = tenantFutureLeave,
            periods = ledger.periods,
            asOfDate = LocalDate.of(2026, 1, 2)
        )
        assertThat(nextDateFuture).isEqualTo(LocalDate.of(2026, 1, 5))

        // 6. Portfolio summary with tenant having past leaveDate, future leaveDate, null leaveDate, and archived
        val tPastLeave = tenant.copy(id = "past", leaveDate = LocalDate.of(2025, 12, 31))
        val tArchived = tenant.copy(id = "arch", isArchived = true)
        val tNullLeave = tenant.copy(id = "nullL", leaveDate = null)
        val tFutureLeave2 = tenant.copy(id = "futL", leaveDate = LocalDate.of(2027, 12, 31))

        val summary = RentCalculator.calculatePortfolioSummary(
            tenants = listOf(tPastLeave, tArchived, tNullLeave, tFutureLeave2),
            ledgers = emptyList(),
            asOfDate = LocalDate.of(2026, 5, 1)
        )
        assertThat(summary.activeTenantsCount).isEqualTo(2) // tNullLeave and tFutureLeave2
    }
}
