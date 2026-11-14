package com.tenantmanager.ui.properties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.tenantmanager.domain.model.Property
import com.tenantmanager.ui.components.ConfirmDialog
import com.tenantmanager.ui.components.EmptyStateView
import com.tenantmanager.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    viewModel: PropertiesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var propertyToEdit by remember { mutableStateOf<Property?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var propertyToDelete by remember { mutableStateOf<PropertyWithUnitCount?>(null) }

    val symbol = uiState.settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Properties", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                propertyToEdit = null
                showAddEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Property")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.properties.isEmpty()) {
            EmptyStateView(
                title = "No Properties Added",
                description = "Group your tenants under buildings or properties (e.g. Sunrise Apartments).",
                icon = Icons.Default.Apartment,
                actionButtonText = "+ Add Property",
                onActionClick = {
                    propertyToEdit = null
                    showAddEditDialog = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.properties, key = { it.property.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.property.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!item.property.address.isNullOrBlank()) {
                                    Text(
                                        text = item.property.address!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.unitCount} tenant(s) linked" +
                                            if (item.property.defaultRent.isPositive) " • Base Rent: ${Formatters.formatCurrency(item.property.defaultRent, symbol)}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                IconButton(onClick = {
                                    propertyToEdit = item.property
                                    showAddEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit property ${item.property.name}")
                                }
                                IconButton(onClick = { propertyToDelete = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete property ${item.property.name}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditPropertyDialog(
            property = propertyToEdit,
            currencySymbol = symbol,
            onConfirm = { name, address, rent ->
                viewModel.addOrUpdateProperty(propertyToEdit?.id, name, address, rent)
                showAddEditDialog = false
            },
            onDismiss = { showAddEditDialog = false }
        )
    }

    if (propertyToDelete != null) {
        ConfirmDialog(
            title = "Delete Property?",
            message = "Deleting \"${propertyToDelete!!.property.name}\" will unlink ${propertyToDelete!!.unitCount} tenant(s). Tenant records and payments will NOT be deleted.",
            confirmButtonText = "Delete Property",
            isDestructive = true,
            onConfirm = {
                val id = propertyToDelete!!.property.id
                propertyToDelete = null
                viewModel.deleteProperty(id)
            },
            onDismiss = { propertyToDelete = null }
        )
    }
}

@Composable
private fun AddEditPropertyDialog(
    property: Property?,
    currencySymbol: String,
    onConfirm: (name: String, address: String?, defaultRentRupees: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(property?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf(property?.address ?: "") }
    var rentText by remember { mutableStateOf(property?.defaultRent?.let { (it.minorUnits / 100).toString() } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (property == null) "Add Property" else "Edit Property", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "Property name is required" else null
                    },
                    label = { Text("Property Name *") },
                    placeholder = { Text("e.g. Greenwood Apartments") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Default Base Rent ($currencySymbol) (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    nameError = "Property name is required"
                    return@Button
                }
                val rentVal = rentText.toLongOrNull() ?: 0L
                onConfirm(name, address, rentVal)
            }) {
                Text(if (property == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

