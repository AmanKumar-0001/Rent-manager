package com.tenantmanager.domain.sync

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SheetRowMapperTest {

    private val property = Property(
        id = "p-1",
        name = "Green Villa",
        address = "Park Avenue 4",
        defaultRent = Money.ofRupees(18000)
    )

    private val tenant = Tenant(
        id = "t-1",
        propertyId = "p-1",
        name = "Anil Kapoor",
        phone = "9822001122",
        email = "anil@example.com",
        unitName = "Villa 2",
        joinDate = LocalDate.of(2026, 1, 1),
        leaveDate = null,
        monthlyRent = Money.ofRupees(18000),
        dueDay = 5,
        securityDeposit = Money.ofRupees(36000),
        notes = "Prompt payer"
    )

    private val payment = Payment(
        id = "pay-1",
        tenantId = "t-1",
        amount = Money.ofRupees(18000),
        paidDate = LocalDate.of(2026, 1, 5),
        periodMonth = YearMonth.of(2026, 1),
        notes = "Cash receipt"
    )

    @Test
    fun `mapToTabs produces exactly 3 tabs with correct headers and rows without notes`() {
        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment),
            includeNotes = false,
            currencySymbol = "₹"
        )

        val tabs = SheetRowMapper.mapToTabs(snapshot)

        assertThat(tabs).hasSize(3)

        // Tenants tab
        val tenantsTab = tabs[0]
        assertThat(tenantsTab.title).isEqualTo(SheetRowMapper.TAB_TENANTS)
        assertThat(tenantsTab.headers).doesNotContain("Notes")
        assertThat(tenantsTab.rows).hasSize(1)
        val tenantRow = tenantsTab.rows[0]
        assertThat(tenantRow[0]).isEqualTo("t-1")
        assertThat(tenantRow[1]).isEqualTo("Green Villa")
        assertThat(tenantRow[3]).isEqualTo("Anil Kapoor")
        assertThat(tenantRow[6]).isEqualTo("₹18,000")
        assertThat(tenantRow[10]).isEqualTo("NO") // Archived = NO
        assertThat(tenantRow[11]).isEqualTo("₹36,000") // Deposit

        // Payments tab
        val paymentsTab = tabs[1]
        assertThat(paymentsTab.title).isEqualTo(SheetRowMapper.TAB_PAYMENTS)
        assertThat(paymentsTab.headers).doesNotContain("Notes")
        assertThat(paymentsTab.rows).hasSize(1)
        val paymentRow = paymentsTab.rows[0]
        assertThat(paymentRow[0]).isEqualTo("pay-1")
        assertThat(paymentRow[1]).isEqualTo("Anil Kapoor")
        assertThat(paymentRow[3]).isEqualTo("2026-01")
        assertThat(paymentRow[4]).isEqualTo("₹18,000")

        // Properties tab
        val propertiesTab = tabs[2]
        assertThat(propertiesTab.title).isEqualTo(SheetRowMapper.TAB_PROPERTIES)
        assertThat(propertiesTab.rows).hasSize(1)
        val propRow = propertiesTab.rows[0]
        assertThat(propRow[0]).isEqualTo("p-1")
        assertThat(propRow[1]).isEqualTo("Green Villa")
        assertThat(propRow[3]).isEqualTo("₹18,000")
    }

    @Test
    fun `mapToTabs includes Notes column when includeNotes is true`() {
        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment),
            includeNotes = true,
            currencySymbol = "₹"
        )

        val tabs = SheetRowMapper.mapToTabs(snapshot)

        val tenantsTab = tabs[0]
        assertThat(tenantsTab.headers).contains("Notes")
        val tenantRow = tenantsTab.rows[0]
        assertThat(tenantRow.last()).isEqualTo("Prompt payer")

        val paymentsTab = tabs[1]
        assertThat(paymentsTab.headers).contains("Notes")
        val paymentRow = paymentsTab.rows[0]
        assertThat(paymentRow.last()).isEqualTo("Cash receipt")
    }
}

