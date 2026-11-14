package com.tenantmanager.ui.tenants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.ui.components.EmptyStateView
import com.tenantmanager.ui.components.StatusChip
import com.tenantmanager.ui.util.Formatters

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
                placeholder = { Text("Search by name or unit...") },
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
                                TenantFilter.ACTIVE -> "Active"
                                TenantFilter.OVERDUE -> "Overdue"
                                TenantFilter.DUE_SOON -> "Due Soon"
                                TenantFilter.LEFT -> "Left"
                            }
                            Text(label)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.ledgers.isEmpty()) {
                EmptyStateView(
                    title = if (uiState.searchQuery.isNotBlank()) "No Matching Tenants" else "No Tenants Found",
                    description = if (uiState.searchQuery.isNotBlank()) "Try refining your search or changing the filter." else "Add your first tenant using the + button.",
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
                            ledger = ledger,
                            symbol = symbol,
                            onCardClick = { onNavigateToTenantDetail(ledger.tenant.id) },
                            onRecordPayment = { onRecordPayment(ledger.tenant.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TenantCard(
    ledger: TenantLedger,
    symbol: String,
    onCardClick: () -> Unit,
    onRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ledger.tenant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unit: ${ledger.tenant.unitName} • Rent: ${Formatters.formatCurrency(ledger.tenant.monthlyRent, symbol)}/mo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = ledger.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next Payment: ${Formatters.formatDate(ledger.nextPaymentDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (ledger.totalBalance.isPositive) {
                        Text(
                            text = "Unpaid Balance: ${Formatters.formatCurrency(ledger.totalBalance, symbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay", fontSize = 13.sp)
                }
            }
        }
    }
}

