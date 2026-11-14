package com.tenantmanager.ui.tenants

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.domain.model.RentalPeriod
import com.tenantmanager.ui.components.ConfirmDialog
import com.tenantmanager.ui.components.PeriodStatusChip
import com.tenantmanager.ui.components.StatusChip
import com.tenantmanager.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailScreen(
    viewModel: TenantDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onRecordPayment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showMarkAsLeftDialog by remember { mutableStateOf(false) }
    var paymentToDeleteId by remember { mutableStateOf<String?>(null) }

    val ledger = uiState.ledger
    val tenant = ledger?.tenant
    val symbol = uiState.settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tenant?.name ?: "Tenant Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tenant != null) {
                        IconButton(onClick = { onNavigateToEdit(tenant.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Tenant")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (!tenant.isArchived) {
                                DropdownMenuItem(
                                    text = { Text("Mark as Left (Archive)") },
                                    onClick = {
                                        showMenu = false
                                        showMarkAsLeftDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (tenant != null && !tenant.isArchived) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { onRecordPayment(tenant.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Payment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (ledger == null || tenant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tenant not found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile & Status Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tenant.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unit ${tenant.unitName}" + (uiState.property?.let { " • ${it.name}" } ?: ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusChip(status = ledger.status)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contact Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 12.sp)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${tenant.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS", fontSize = 12.sp)
                                }

                                if (!tenant.email.isNullOrBlank()) {
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${tenant.email}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Email", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Rental Agreement Details
                item {
                    Text("Rental Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailRow("Monthly Rent", Formatters.formatCurrency(tenant.monthlyRent, symbol))
                            DetailRow("Payment Due Day", "${tenant.dueDay}${getDaySuffix(tenant.dueDay)} of month" + if (tenant.dueDay == 31) " (Last Day)" else "")
                            DetailRow("Joined On", Formatters.formatDate(tenant.joinDate))
                            if (tenant.leaveDate != null) {
                                DetailRow("Leave Date", Formatters.formatDate(tenant.leaveDate))
                            }
                            if (tenant.securityDeposit.isPositive) {
                                DetailRow("Security Deposit", Formatters.formatCurrency(tenant.securityDeposit, symbol))
                            }
                            tenant.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                DetailRow("Notes", notes)
                            }
                            DetailRow("Total Outstanding Balance", Formatters.formatCurrency(ledger.totalBalance, symbol))
                        }
                    }
                }

                // Month-by-month Ledger Table
                item {
                    Text("Month-by-Month Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(ledger.periods.reversed()) { period ->
                    RentalPeriodCard(period = period, symbol = symbol)
                }

                // Payment History
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Payment Records (${uiState.payments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (uiState.payments.isEmpty()) {
                    item {
                        Text(
                            text = "No payments recorded yet for this tenant.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.payments, key = { it.id }) { payment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Formatters.formatCurrency(payment.amount, symbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Paid on ${Formatters.formatDate(payment.paidDate)} • Period: ${payment.periodMonth}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!payment.notes.isNullOrBlank()) {
                                        Text(
                                            text = "Note: ${payment.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { paymentToDeleteId = payment.id }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete payment",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
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

    if (showMarkAsLeftDialog) {
        ConfirmDialog(
            title = "Mark Tenant as Left?",
            message = "This will archive the tenant and stop future rent periods. Historical payment records and ledgers are preserved.",
            confirmButtonText = "Mark as Left",
            onConfirm = {
                showMarkAsLeftDialog = false
                viewModel.markAsLeft()
            },
            onDismiss = { showMarkAsLeftDialog = false }
        )
    }

    if (paymentToDeleteId != null) {
        ConfirmDialog(
            title = "Delete Payment Record?",
            message = "Are you sure you want to delete this payment record? The tenant's ledger balance will be recalculated immediately.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                val id = paymentToDeleteId
                paymentToDeleteId = null
                if (id != null) {
                    viewModel.deletePayment(id)
                }
            },
            onDismiss = { paymentToDeleteId = null }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RentalPeriodCard(period: RentalPeriod, symbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatPeriodMonth(period.periodMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PeriodStatusChip(status = period.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Due: ${Formatters.formatDate(period.dueDate)}", style = MaterialTheme.typography.bodySmall)
                Text("Expected: ${Formatters.formatCurrency(period.expectedRent, symbol)}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Paid: ${Formatters.formatCurrency(period.totalPaid, symbol)}" + if (period.creditApplied.isPositive) " (incl. credit ${Formatters.formatCurrency(period.creditApplied, symbol)})" else "", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Balance: ${Formatters.formatCurrency(period.balance, symbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (period.balance.isPositive) FontWeight.Bold else FontWeight.Normal,
                    color = if (period.balance.isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getDaySuffix(day: Int): String {
    return when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}

