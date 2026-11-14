package com.tenantmanager.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenantmanager.ui.components.ConfirmDialog
import com.tenantmanager.ui.components.DangerZoneTypedConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportPassphraseDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showWipeDataDialog by remember { mutableStateOf(false) }

    // File exporters
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val createPlaintextFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportJson != null) {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
            pendingExportJson = null
        }
    }

    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val createEncryptedFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && pendingExportBytes != null) {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(pendingExportBytes!!)
            }
            Toast.makeText(context, "Encrypted backup exported successfully!", Toast.LENGTH_SHORT).show()
            pendingExportBytes = null
        }
    }

    // File importer
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            if (content != null) {
                pendingImportContent = content
                showImportConfirmDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backupEvent.collect { event ->
            when (event) {
                is BackupEvent.ExportPlaintextReady -> {
                    pendingExportJson = event.json
                    createPlaintextFileLauncher.launch("tenant_manager_backup_${System.currentTimeMillis()}.json")
                }
                is BackupEvent.ExportEncryptedReady -> {
                    pendingExportBytes = event.bytes
                    createEncryptedFileLauncher.launch("tenant_manager_backup_${System.currentTimeMillis()}.enc")
                }
                is BackupEvent.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is BackupEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings
            Text("General Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Currency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCurrencyDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Column {
                                Text("Currency Symbol", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Format: ${settings.currencySymbol}1,25,000", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(settings.currencySymbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    HorizontalDivider()

                    // Grace Days Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grace Days: ${settings.graceDays} days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value = settings.graceDays.toFloat(),
                            onValueChange = { viewModel.updateGraceDays(it.toInt()) },
                            valueRange = 0f..15f,
                            steps = 14
                        )
                        Text("Number of buffer days after due date before tenant is marked OVERDUE.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider()

                    // Reminder Hour
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Reminder Hour: ${settings.reminderHour}:00", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value = settings.reminderHour.toFloat(),
                            onValueChange = { viewModel.updateReminderHour(it.toInt()) },
                            valueRange = 6f..22f,
                            steps = 15
                        )
                        Text("Approximate local hour for daily rent notification check.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Security
            Text("Security & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Column {
                                Text("App Lock (Biometric / PIN)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Require biometric or screen lock on app open", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = settings.appLockEnabled,
                            onCheckedChange = viewModel::updateAppLock
                        )
                    }
                }
            }

            // Manage Properties link
            Text("Organization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToProperties)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Column {
                            Text("Manage Properties", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Add, rename, or organize rental buildings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.padding(4.dp))
                }
            }

            // Backup & Restore
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = viewModel::exportPlaintextBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Export Backup (JSON)")
                    }

                    Button(
                        onClick = { showExportPassphraseDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Export Encrypted Backup (AES-GCM)")
                    }

                    Button(
                        onClick = { openFileLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Restore from Backup")
                    }
                }
            }

            // Danger Zone
            Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wipe All Local Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Permanently delete all tenants, rental properties, and payment histories from this device. This action cannot be reversed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showWipeDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Wipe All Application Data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectorDialog(
            current = settings.currencySymbol,
            onSelect = {
                viewModel.updateCurrencySymbol(it)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showExportPassphraseDialog) {
        PassphraseInputDialog(
            title = "Encrypted Backup Passphrase",
            description = "Enter a strong passphrase to encrypt your backup with AES-256-GCM. You will need this exact passphrase to restore it.",
            onConfirm = { pass ->
                showExportPassphraseDialog = false
                viewModel.exportEncryptedBackup(pass)
            },
            onDismiss = { showExportPassphraseDialog = false }
        )
    }

    if (showImportConfirmDialog && pendingImportContent != null) {
        ConfirmDialog(
            title = "Restore Backup?",
            message = "Restoring this backup will completely replace all existing tenants, properties, and payments on this device. This cannot be undone.",
            confirmButtonText = "Restore & Replace",
            isDestructive = true,
            onConfirm = {
                val content = pendingImportContent!!
                pendingImportContent = null
                showImportConfirmDialog = false
                viewModel.importPlaintextBackup(content)
            },
            onDismiss = {
                pendingImportContent = null
                showImportConfirmDialog = false
            }
        )
    }

    if (showWipeDataDialog) {
        DangerZoneTypedConfirmDialog(
            title = "Wipe All Data?",
            message = "This will permanently remove all properties, tenants, and payment records from your device's SQLite database.",
            expectedKeyword = "DELETE",
            confirmButtonText = "Wipe Everything",
            onConfirm = {
                showWipeDataDialog = false
                viewModel.wipeAllData()
            },
            onDismiss = { showWipeDataDialog = false }
        )
    }
}

@Composable
private fun CurrencySelectorDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customSymbol by remember { mutableStateOf(current) }
    val presets = listOf("₹", "$", "€", "£", "AED", "Rs")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency Symbol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { sym ->
                            Button(
                                onClick = { onSelect(sym) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(sym, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customSymbol,
                    onValueChange = { customSymbol = it },
                    label = { Text("Custom Symbol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (customSymbol.isNotBlank()) onSelect(customSymbol) }) {
                Text("Save Custom")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PassphraseInputDialog(
    title: String,
    description: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    var passConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passConfirm,
                    onValueChange = { passConfirm = it },
                    label = { Text("Confirm Passphrase") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pass.length < 6) {
                    error = "Passphrase must be at least 6 characters"
                    return@Button
                }
                if (pass != passConfirm) {
                    error = "Passphrases do not match"
                    return@Button
                }
                onConfirm(pass)
            }) {
                Text("Export Encrypted")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

