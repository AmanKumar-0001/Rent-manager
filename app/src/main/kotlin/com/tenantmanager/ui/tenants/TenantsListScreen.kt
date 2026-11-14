package com.tenantmanager.ui.tenants

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.ui.components.ConfirmationDialog
import com.tenantmanager.ui.components.EmptyState
import com.tenantmanager.ui.components.TenantCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantsListScreen(
    viewModel: TenantsViewModel,
    onNavigateToTenantDetail: (String) -> Unit,
    onNavigateToAddTenant: () -> Unit,
    onNavigateToProperties: () -> Unit,
    onRecordPayment: (tenantId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val symbol = uiState.settings.currencySymbol
    var tenantToArchive by remember { mutableStateOf<Tenant?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tenants", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToProperties) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "Manage Properties"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTenant) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Tenant")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search by name, unit, or property...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenantFilter.values().forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = {
                            val label = when (filter) {
                                TenantFilter.ALL -> "All"
                                TenantFilter.OVERDUE -> "Overdue"
                                TenantFilter.DUE_SOON -> "Due Soon"
                                TenantFilter.PAID -> "Paid"
                                TenantFilter.LEFT -> "Left"
                            }
                            Text(label)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.ledgers.isEmpty()) {
                EmptyState(
                    title = if (uiState.searchQuery.isNotBlank()) "No Matching Tenants" else "No Tenants Found",
                    description = if (uiState.searchQuery.isNotBlank()) {
                        "Try refining your search query or changing the active filter."
                    } else {
                        "Add your first tenant to track rent periods, payments, and balances."
                    },
                    icon = Icons.Default.People,
                    actionButtonText = if (uiState.searchQuery.isBlank() && uiState.selectedFilter == TenantFilter.ALL) "+ Add Tenant" else null,
                    onActionClick = onNavigateToAddTenant,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.ledgers, key = { it.tenant.id }) { ledger ->
                        TenantCard(
                            tenant = ledger.tenant,
                            propertyName = uiState.propertyNames[ledger.tenant.propertyId],
                            status = ledger.status,
                            nextPaymentDate = ledger.nextPaymentDate,
                            currencySymbol = symbol,
                            onClick = { onNavigateToTenantDetail(ledger.tenant.id) },
                            onRecordPayment = { onRecordPayment(ledger.tenant.id) },
                            onArchive = { tenantToArchive = ledger.tenant }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Confirmation dialog for archiving tenant
    tenantToArchive?.let { tenant ->
        ConfirmationDialog(
            title = "Archive Tenant",
            message = "Are you sure you want to archive ${tenant.name}? Rent calculation will stop as of today. You can reactivate this tenant later by editing their details.",
            confirmButtonText = "Archive",
            isDestructive = false,
            onConfirm = {
                viewModel.archiveTenant(tenant.id)
                tenantToArchive = null
            },
            onDismiss = { tenantToArchive = null }
        )
    }
}
