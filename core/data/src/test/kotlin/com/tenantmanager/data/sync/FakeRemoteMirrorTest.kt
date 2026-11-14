package com.tenantmanager.data.sync

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.sync.SheetRowMapper
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FakeRemoteMirrorTest {

    private lateinit var fakeApiClient: FakeSheetsApiClient
    private lateinit var remoteMirror: SheetsRemoteMirror

    private val property = Property(
        id = "prop-101",
        name = "Lotus Apartment",
        address = "14 MG Road, Bengaluru",
        defaultRent = Money.ofRupees(22000)
    )

    private val tenant = Tenant(
        id = "ten-201",
        propertyId = "prop-101",
        name = "Rajesh Sharma",
        phone = "9876543210",
        email = "rajesh@example.com",
        unitName = "Flat 4A",
        joinDate = LocalDate.of(2026, 1, 1),
        leaveDate = null,
        monthlyRent = Money.ofRupees(22000),
        dueDay = 5,
        securityDeposit = Money.ofRupees(44000),
        notes = "Aadhaar: 1111 2222 3333, Bank AC 9876543210123456"
    )

    private val payment = Payment(
        id = "pay-301",
        tenantId = "ten-201",
        amount = Money.ofRupees(22000),
        paidDate = LocalDate.of(2026, 1, 5),
        periodMonth = YearMonth.of(2026, 1),
        notes = "Online IMPS"
    )

    @Before
    fun setUp() {
        fakeApiClient = FakeSheetsApiClient()
        remoteMirror = SheetsRemoteMirror(fakeApiClient)
    }

    @Test
    fun `sync executes ONE batchUpdate request replacing all 3 tabs`() = runTest {
        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment),
            includeNotes = false,
            currencySymbol = "₹"
        )

        val result = remoteMirror.sync(snapshot, accessToken = "test-token-abc")

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        val success = result as SyncResult.Success
        assertThat(success.spreadsheetId).isEqualTo("fake-sheet-id-12345")
        assertThat(success.itemsCount).isEqualTo(3)

        // Verifies ONE batchUpdate call was made (not per-row calls)
        assertThat(fakeApiClient.batchUpdateCallsCount).isEqualTo(1)

        // Verifies all 3 tabs exist in the batch
        assertThat(fakeApiClient.uploadedTabs.keys).containsExactly(
            SheetRowMapper.TAB_TENANTS,
            SheetRowMapper.TAB_PAYMENTS,
            SheetRowMapper.TAB_PROPERTIES
        )

        // Check Tenants tab data
        val tenantsTab = fakeApiClient.uploadedTabs[SheetRowMapper.TAB_TENANTS]!!
        assertThat(tenantsTab.headers).doesNotContain("Notes")
        assertThat(tenantsTab.rows).hasSize(1)
        assertThat(tenantsTab.rows[0][0]).isEqualTo("ten-201")
        assertThat(tenantsTab.rows[0][1]).isEqualTo("Lotus Apartment")
        assertThat(tenantsTab.rows[0][3]).isEqualTo("Rajesh Sharma")
        assertThat(tenantsTab.rows[0][6]).isEqualTo("₹22,000")

        // Check Payments tab data
        val paymentsTab = fakeApiClient.uploadedTabs[SheetRowMapper.TAB_PAYMENTS]!!
        assertThat(paymentsTab.rows).hasSize(1)
        assertThat(paymentsTab.rows[0][0]).isEqualTo("pay-301")
        assertThat(paymentsTab.rows[0][1]).isEqualTo("Rajesh Sharma")
        assertThat(paymentsTab.rows[0][4]).isEqualTo("₹22,000")

        // Check Properties tab data
        val propertiesTab = fakeApiClient.uploadedTabs[SheetRowMapper.TAB_PROPERTIES]!!
        assertThat(propertiesTab.rows).hasSize(1)
        assertThat(propertiesTab.rows[0][0]).isEqualTo("prop-101")
        assertThat(propertiesTab.rows[0][1]).isEqualTo("Lotus Apartment")
    }

    @Test
    fun `sync applies privacy filter to sanitize notes when includeNotes is enabled`() = runTest {
        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment),
            includeNotes = true,
            currencySymbol = "₹"
        )

        val result = remoteMirror.sync(snapshot, accessToken = "test-token-abc")

        assertThat(result).isInstanceOf(SyncResult.Success::class.java)

        val tenantsTab = fakeApiClient.uploadedTabs[SheetRowMapper.TAB_TENANTS]!!
        assertThat(tenantsTab.headers).contains("Notes")
        val tenantRow = tenantsTab.rows[0]
        val uploadedNote = tenantRow.last()
        assertThat(uploadedNote).doesNotContain("1111 2222 3333")
        assertThat(uploadedNote).contains("[REDACTED_AADHAAR]")
        assertThat(uploadedNote).doesNotContain("9876543210123456")
        assertThat(uploadedNote).contains("[REDACTED_ACCOUNT]")
    }

    @Test
    fun `sync handles authorization revocation quietly with isAuthRevoked true`() = runTest {
        fakeApiClient.shouldFailWithAuthRevoked = true

        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment)
        )

        val result = remoteMirror.sync(snapshot, accessToken = "revoked-token")

        assertThat(result).isInstanceOf(SyncResult.Failure::class.java)
        val failure = result as SyncResult.Failure
        assertThat(failure.isAuthRevoked).isTrue()
        assertThat(failure.error).contains("expired or revoked")
    }

    @Test
    fun `sync handles network failure quietly`() = runTest {
        fakeApiClient.shouldFailWithNetworkError = true

        val snapshot = SyncSnapshot(
            properties = listOf(property),
            tenants = listOf(tenant),
            payments = listOf(payment)
        )

        val result = remoteMirror.sync(snapshot, accessToken = "valid-token")

        assertThat(result).isInstanceOf(SyncResult.Failure::class.java)
        val failure = result as SyncResult.Failure
        assertThat(failure.isAuthRevoked).isFalse()
        assertThat(failure.error).contains("Network error")
    }
}

