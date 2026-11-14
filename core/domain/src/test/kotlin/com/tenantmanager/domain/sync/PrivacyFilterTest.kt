package com.tenantmanager.domain.sync

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PrivacyFilterTest {

    private val sampleTenant = Tenant(
        id = "t1",
        propertyId = "p1",
        name = "Kavita Sharma",
        phone = "9876543210",
        email = "kavita@example.com",
        unitName = "Flat 101",
        joinDate = LocalDate.of(2026, 1, 1),
        monthlyRent = Money.ofRupees(25000),
        dueDay = 5,
        notes = "Aadhaar: 1234 5678 9012, PAN: ABCDE1234F, Bank Account 123456789012345 IFSC: HDFC0001234. Regular tenant."
    )

    private val samplePayment = Payment(
        id = "pay1",
        tenantId = "t1",
        amount = Money.ofRupees(25000),
        paidDate = LocalDate.of(2026, 1, 5),
        periodMonth = YearMonth.of(2026, 1),
        notes = "Paid via NEFT from AC 98765432109876 IFSC SBIN0004321, PAN ABCDE9876Z"
    )

    private val sampleProperty = Property(
        id = "p1",
        name = "Sunshine Heights",
        address = "12 MG Road",
        defaultRent = Money.ofRupees(25000)
    )

    @Test
    fun `when includeNotes is false, all notes are stripped to null`() {
        val snapshot = SyncSnapshot(
            properties = listOf(sampleProperty),
            tenants = listOf(sampleTenant),
            payments = listOf(samplePayment),
            includeNotes = false
        )

        val filtered = PrivacyFilter.filterSnapshot(snapshot)

        assertThat(filtered.tenants[0].notes).isNull()
        assertThat(filtered.payments[0].notes).isNull()
    }

    @Test
    fun `when includeNotes is true, government IDs and bank details are redacted from notes`() {
        val snapshot = SyncSnapshot(
            properties = listOf(sampleProperty),
            tenants = listOf(sampleTenant),
            payments = listOf(samplePayment),
            includeNotes = true
        )

        val filtered = PrivacyFilter.filterSnapshot(snapshot)

        val tenantNotes = filtered.tenants[0].notes!!
        assertThat(tenantNotes).doesNotContain("1234 5678 9012")
        assertThat(tenantNotes).contains("[REDACTED_AADHAAR]")
        assertThat(tenantNotes).doesNotContain("ABCDE1234F")
        assertThat(tenantNotes).contains("[REDACTED_PAN]")
        assertThat(tenantNotes).doesNotContain("123456789012345")
        assertThat(tenantNotes).contains("[REDACTED_ACCOUNT]")
        assertThat(tenantNotes).doesNotContain("HDFC0001234")
        assertThat(tenantNotes).contains("[REDACTED_IFSC]")
        assertThat(tenantNotes).contains("Regular tenant.")

        val paymentNotes = filtered.payments[0].notes!!
        assertThat(paymentNotes).doesNotContain("987654321098")
        assertThat(paymentNotes).contains("[REDACTED_ACCOUNT]")
        assertThat(paymentNotes).doesNotContain("SBIN0004321")
        assertThat(paymentNotes).contains("[REDACTED_IFSC]")
        assertThat(paymentNotes).doesNotContain("ABCDE9876Z")
        assertThat(paymentNotes).contains("[REDACTED_PAN]")
    }
}
