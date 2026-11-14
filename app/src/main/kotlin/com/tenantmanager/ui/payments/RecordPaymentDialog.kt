package com.tenantmanager.ui.payments

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.ui.components.HapticHelper
import com.tenantmanager.ui.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    ledger: TenantLedger,
    currencySymbol: String = "₹",
    hapticFeedbackEnabled: Boolean = true,
    onConfirmPayment: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val tenant = ledger.tenant
    val earliestUnpaidPeriod = ledger.periods.firstOrNull { it.balance.isPositive }
    val initialPeriod = earliestUnpaidPeriod?.periodMonth ?: YearMonth.now()
    val prefilledAmount = earliestUnpaidPeriod?.balance ?: tenant.monthlyRent

    var amountText by remember { mutableStateOf((prefilledAmount.minorUnits / 100).toString()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }
    var notes by remember { mutableStateOf("") }

    var periodDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val candidatePeriods = remember(ledger) {
        val current = YearMonth.now()
        val list = ledger.periods.map { it.periodMonth }.toMutableList()
        if (current !in list) list.add(current)
        val next = current.plusMonths(1)
        if (next !in list) list.add(next)
        list.distinct().sortedDescending()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Record Rent Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tenant.name} (${tenant.unitName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick-select chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fullRentVal = (tenant.monthlyRent.minorUnits / 100).toString()
                    FilterChip(
                        selected = amountText == fullRentVal,
                        onClick = {
                            amountText = fullRentVal
                            amountError = null
                        },
                        label = { Text("Full Rent (${currencySymbol}${fullRentVal})") }
                    )

                    if (earliestUnpaidPeriod != null && earliestUnpaidPeriod.balance.isPositive &&
                        earliestUnpaidPeriod.balance != tenant.monthlyRent
                    ) {
                        val remainingVal = (earliestUnpaidPeriod.balance.minorUnits / 100).toString()
                        FilterChip(
                            selected = amountText == remainingVal,
                            onClick = {
                                amountText = remainingVal
                                amountError = null
                            },
                            label = { Text("Remaining Due (${currencySymbol}${remainingVal})") }
                        )
                    }

                    FilterChip(
                        selected = amountText == "5000",
                        onClick = {
                            amountText = "5000"
                            amountError = null
                        },
                        label = { Text("${currencySymbol}5,000") }
                    )

                    FilterChip(
                        selected = amountText == "10000",
                        onClick = {
                            amountText = "10000"
                            amountError = null
                        },
                        label = { Text("${currencySymbol}10,000") }
                    )
                }

                // Amount TextField with tabular bold styling
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val parsed = it.toLongOrNull()
                        amountError = if (parsed == null || parsed <= 0) "Amount must be greater than 0" else null
                    },
                    label = { Text("Payment Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )

                // Period Dropdown
                ExposedDropdownMenuBox(
                    expanded = periodDropdownExpanded,
                    onExpandedChange = { periodDropdownExpanded = !periodDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = Formatters.formatPeriodMonth(selectedPeriod),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rent Period Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = periodDropdownExpanded,
                        onDismissRequest = { periodDropdownExpanded = false }
                    ) {
                        candidatePeriods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(Formatters.formatPeriodMonth(period)) },
                                onClick = {
                                    selectedPeriod = period
                                    periodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Payment Date: ${Formatters.formatDisplayDate(selectedDate)}")
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. UPI Ref / Cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toLongOrNull()
                    if (amountVal == null || amountVal <= 0) {
                        amountError = "Amount must be greater than 0"
                        return@Button
                    }

                    // Haptic feedback
                    HapticHelper.performConfirm(view, hapticFeedbackEnabled)

                    val payment = Payment(
                        id = UUID.randomUUID().toString(),
                        tenantId = tenant.id,
                        amount = Money.ofRupees(amountVal),
                        paidDate = selectedDate,
                        periodMonth = selectedPeriod,
                        notes = notes.trim().ifBlank { null },
                        lastModified = System.currentTimeMillis()
                    )
                    onConfirmPayment(payment)
                }
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Modal Bottom Sheet variant
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentBottomSheet(
    ledger: TenantLedger,
    currencySymbol: String = "₹",
    hapticFeedbackEnabled: Boolean = true,
    onConfirmPayment: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
    RecordPaymentDialog(
        ledger = ledger,
        currencySymbol = currencySymbol,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        onConfirmPayment = onConfirmPayment,
        onDismiss = onDismiss
    )
}
