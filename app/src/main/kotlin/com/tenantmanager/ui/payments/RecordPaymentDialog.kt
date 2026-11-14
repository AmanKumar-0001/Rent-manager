package com.tenantmanager.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.TenantLedger
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
    onConfirmPayment: (Payment) -> Unit,
    onDismiss: () -> Unit
) {
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

    // List of candidate periods: all periods from joinDate to current+1
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
                Text("Record Rent Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${tenant.name} (${tenant.unitName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Amount
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
                            .menuAnchor()
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
                    Text("Payment Date: ${Formatters.formatDate(selectedDate)}")
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

