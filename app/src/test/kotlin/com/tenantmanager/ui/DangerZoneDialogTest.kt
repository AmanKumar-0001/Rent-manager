package com.tenantmanager.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.tenantmanager.ui.components.DangerZoneTypedConfirmDialog
import com.tenantmanager.ui.theme.TenantManagerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DangerZoneDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `danger zone wipe requires exact typed confirmation`() {
        var confirmed = false

        composeTestRule.setContent {
            TenantManagerTheme {
                DangerZoneTypedConfirmDialog(
                    title = "Confirm Data Wipe",
                    message = "This will erase everything.",
                    expectedKeyword = "DELETE EVERYTHING",
                    confirmButtonText = "Permanently Delete",
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }

        // Initially, the confirm button is disabled
        composeTestRule.onNode(hasText("Permanently Delete") and hasClickAction())
            .assertIsNotEnabled()

        // Typing incorrect phrase does not enable it
        composeTestRule.onNodeWithText("Type \"DELETE EVERYTHING\" below to confirm:")
            .assertIsDisplayed()
        
        // Find text input by clicking and entering text
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("delete everything")

        // Case-sensitive check fails
        composeTestRule.onNode(hasText("Permanently Delete") and hasClickAction())
            .assertIsNotEnabled()

        // Enter exact match
        composeTestRule.onNode(hasSetTextAction())
            .performTextClearance()
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("DELETE EVERYTHING")

        composeTestRule.onNode(hasText("Permanently Delete") and hasClickAction())
            .assertIsEnabled()
        composeTestRule.onNode(hasText("Permanently Delete") and hasClickAction())
            .performClick()

        assert(confirmed) { "Expected danger zone action to be confirmed" }
    }
}

