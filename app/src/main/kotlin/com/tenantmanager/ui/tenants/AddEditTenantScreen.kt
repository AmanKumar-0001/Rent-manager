package com.tenantmanager.ui.tenants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.ui.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTenantScreen(
    viewModel: AddEditTenantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    var propertyDropdownExpanded by remember { mutableStateOf(false) }

    var showJoinDatePicker by remember { mutableStateOf(false) }
    var showLeaveDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect { success ->
            if (success) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (formState.isEditing) "Edit Tenant" else "Add New Tenant", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Name
            OutlinedTextField(
                value = formState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Tenant Full Name *") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Property Selection Dropdown
            ExposedDropdownMenuBox(
                expanded = propertyDropdownExpanded,
                onExpandedChange = { propertyDropdownExpanded = !propertyDropdownExpanded }
            ) {
                val selectedPropertyName = formState.availableProperties.firstOrNull { it.id == formState.propertyId }?.name ?: "None (Standalone Unit)"
                OutlinedTextField(
                    value = selectedPropertyName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Property / Building") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propertyDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = propertyDropdownExpanded,
                    onDismissRequest = { propertyDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (Standalone Unit)") },
                        onClick = {
                            viewModel.onPropertySelected(null)
                            propertyDropdownExpanded = false
                        }
                    )
                    formState.availableProperties.forEach { prop ->
                        DropdownMenuItem(
                            text = { Text(prop.name) },
                            onClick = {
                                viewModel.onPropertySelected(prop.id)
                                propertyDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Unit Name
            OutlinedTextField(
                value = formState.unitName,
                onValueChange = viewModel::onUnitNameChanged,
                label = { Text("Unit / Flat / Room Number *") },
                placeholder = { Text("e.g. 101, Flat 4B, Room 3") },
                isError = formState.unitNameError != null,
                supportingText = formState.unitNameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone
            OutlinedTextField(
                value = formState.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text("Phone Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = formState.phoneError != null,
                supportingText = formState.phoneError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Email
            OutlinedTextField(
                value = formState.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Email (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Monthly Rent
            OutlinedTextField(
                value = formState.rentRupees,
                onValueChange = viewModel::onRentChanged,
                label = { Text("Monthly Rent Amount (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = formState.rentError != null,
                supportingText = formState.rentError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Due Day Slider (1 to 31)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Due Day", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (formState.dueDay == 31) "31 (Last Day of Month)" else "${formState.dueDay}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = formState.dueDay.toFloat(),
                    onValueChange = { viewModel.onDueDayChanged(it.toInt()) },
                    valueRange = 1f..31f,
                    steps = 29
                )
                Text(
                    text = "If set to 31, rent will always fall on the last calendar day of the month (e.g. Feb 28/29, Apr 30).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showJoinDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Join Date", style = MaterialTheme.typography.labelSmall)
                        Text(Formatters.formatDate(formState.joinDate), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = { showLeaveDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Leave Date", style = MaterialTheme.typography.labelSmall)
                        Text(formState.leaveDate?.let { Formatters.formatDate(it) } ?: "Active (None)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (formState.leaveDateError != null) {
                Text(
                    text = formState.leaveDateError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Security Deposit
            OutlinedTextField(
                value = formState.depositRupees,
                onValueChange = viewModel::onDepositChanged,
                label = { Text("Security Deposit Amount (₹) (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Any lease terms, payment preferences, or details...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = viewModel::saveTenant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (formState.isEditing) "Save Changes" else "Add Tenant", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showJoinDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.joinDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showJoinDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onJoinDateChanged(localDate)
                    }
                    showJoinDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLeaveDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.leaveDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showLeaveDatePicker = false },
            confirmButton = {
                Row {
                    if (formState.leaveDate != null) {
                        TextButton(onClick = {
                            viewModel.onLeaveDateChanged(null)
                            showLeaveDatePicker = false
                        }) {
                            Text("Clear")
                        }
                    }
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.onLeaveDateChanged(localDate)
                        }
                        showLeaveDatePicker = false
                    }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

