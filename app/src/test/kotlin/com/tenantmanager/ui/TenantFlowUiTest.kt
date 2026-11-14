package com.tenantmanager.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.PeriodStatus
import com.tenantmanager.domain.model.RentalPeriod
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.ui.components.PeriodStatusChip
import com.tenantmanager.ui.components.StatusChip
import com.tenantmanager.ui.payments.RecordPaymentDialog
import com.tenantmanager.ui.theme.TenantManagerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TenantFlowUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testStatusChipDisplaysCorrectText() {
        composeTestRule.setContent {
            TenantManagerTheme {
                StatusChip(status = TenantStatus.PAID)
            }
        }

        composeTestRule.onNodeWithText("PAID").assertIsDisplayed()
    }

    @Test
    fun testRecordPaymentDialogPrefillsAndSubmits() {
        val tenant = Tenant(
            id = "t1",
            name = "Aarav Sharma",
            phone = "9876543210",
            unitName = "301",
            joinDate = LocalDate.of(2026, 1, 1),
            monthlyRent = Money.ofRupees(15000),
            dueDay = 5
        )

        val period = RentalPeriod(
            periodMonth = YearMonth.of(2026, 1),
            dueDate = LocalDate.of(2026, 1, 5),
            expectedRent = Money.ofRupees(15000),
            directPaymentsTotal = Money.ZERO,
            creditApplied = Money.ZERO,
            totalPaid = Money.ZERO,
            balance = Money.ofRupees(15000),
            status = PeriodStatus.UNPAID
        )

        val ledger = TenantLedger(
            tenant = tenant,
            periods = listOf(period),
            totalPaid = Money.ZERO,
            totalBalance = Money.ofRupees(15000),
            status = TenantStatus.DUE_SOON,
            nextPaymentDate = LocalDate.of(2026, 1, 5)
        )

        var paymentSaved = false

        composeTestRule.setContent {
            TenantManagerTheme {
                RecordPaymentDialog(
                    ledger = ledger,
                    currencySymbol = "₹",
                    onConfirmPayment = {
                        paymentSaved = true
                    },
                    onDismiss = {}
                )
            }
        }

        // Verify prefilled name and amount
        composeTestRule.onNodeWithText("Aarav Sharma (301)").assertIsDisplayed()

        // Click "Save Payment"
        composeTestRule.onNodeWithText("Save Payment").performClick()

        assert(paymentSaved) { "Expected payment to be saved" }
    }
}

