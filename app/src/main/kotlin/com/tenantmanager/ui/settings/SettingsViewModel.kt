package com.tenantmanager.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.BuildConfig
import com.tenantmanager.auth.GoogleAuthManager
import com.tenantmanager.data.backup.BackupExporter
import com.tenantmanager.data.backup.BackupImporter
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.SheetsSyncSettings
import com.tenantmanager.domain.repository.SyncRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.domain.sync.RemoteMirror
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import com.tenantmanager.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupEvent {
    data class ExportPlaintextReady(val json: String) : BackupEvent()
    data class ExportEncryptedReady(val bytes: ByteArray) : BackupEvent()
    data class LaunchAuthResolution(val intentSender: IntentSender) : BackupEvent()
    data class Success(val message: String) : BackupEvent()
    data class Error(val message: String) : BackupEvent()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository,
    private val paymentRepository: PaymentRepository,
    private val syncRepository: SyncRepository,
    private val remoteMirror: RemoteMirror,
    private val googleAuthManager: GoogleAuthManager,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    val isSyncSupported: Boolean = BuildConfig.SYNC_ENABLED
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val syncSettings: StateFlow<SheetsSyncSettings> = syncRepository.getSyncSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SheetsSyncSettings()
    )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _backupEvent = MutableSharedFlow<BackupEvent>()
    val backupEvent: SharedFlow<BackupEvent> = _backupEvent.asSharedFlow()

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrencySymbol(symbol)
        }
    }

    fun updateGraceDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateGraceDays(days)
        }
    }

    fun updateReminderHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderHour(hour)
        }
    }

    fun updateAppLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAppLockEnabled(enabled)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedbackEnabled(enabled)
        }
    }

    fun exportPlaintextBackup() {
        viewModelScope.launch {
            try {
                val json = backupExporter.exportPlaintextJson()
                _backupEvent.emit(BackupEvent.ExportPlaintextReady(json))
            } catch (e: Exception) {
                _backupEvent.emit(BackupEvent.Error("Export failed: ${e.message}"))
            }
        }
    }

    fun exportEncryptedBackup(passphrase: String) {
        viewModelScope.launch {
            try {
                val bytes = backupExporter.exportEncrypted(passphrase.toCharArray())
                _backupEvent.emit(BackupEvent.ExportEncryptedReady(bytes))
            } catch (e: Exception) {
                _backupEvent.emit(BackupEvent.Error("Export failed: ${e.message}"))
            }
        }
    }

    fun importPlaintextBackup(json: String) {
        viewModelScope.launch {
            try {
                backupImporter.importPlaintextJson(json)
                _backupEvent.emit(BackupEvent.Success("Backup restored successfully!"))
            } catch (e: Exception) {
                _backupEvent.emit(BackupEvent.Error("Import failed: ${e.message}"))
            }
        }
    }

    fun importEncryptedBackup(bytes: ByteArray, passphrase: String) {
        viewModelScope.launch {
            try {
                backupImporter.importEncrypted(bytes, passphrase.toCharArray())
                _backupEvent.emit(BackupEvent.Success("Encrypted backup restored successfully!"))
            } catch (e: Exception) {
                _backupEvent.emit(BackupEvent.Error("Import failed: ${e.message}"))
            }
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            tenantRepository.wipeAllData()
            _backupEvent.emit(BackupEvent.Success("All data permanently wiped."))
        }
    }

    // Google Sheets Sync
    fun signInWithGoogle(activity: Activity? = null) {
        if (!isSyncSupported) return

        viewModelScope.launch {
            val authResult = googleAuthManager.requestAuthorization(activity)
            authResult.onSuccess { result ->
                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent != null) {
                        _backupEvent.emit(BackupEvent.LaunchAuthResolution(pendingIntent.intentSender))
                    } else {
                        _backupEvent.emit(BackupEvent.Error("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."))
                    }
                } else {
                    val token = result.accessToken
                    if (!token.isNullOrBlank()) {
                        val email = result.toGoogleSignInAccount()?.email ?: "Connected Google Account"
                        syncRepository.updateGoogleAccount(true, email)
                        _backupEvent.emit(BackupEvent.Success("Connected to Google Drive & Sheets!"))
                    } else {
                        _backupEvent.emit(BackupEvent.Error("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."))
                    }
                }
            }.onFailure { e ->
                val msg = e.message ?: "Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."
                _backupEvent.emit(BackupEvent.Error(msg))
            }
        }
    }

    fun onAuthorizationResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            val authResult = googleAuthManager.handleAuthorizationResult(resultCode, data)
            authResult.onSuccess { result ->
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    val email = result.toGoogleSignInAccount()?.email ?: "Connected Google Account"
                    syncRepository.updateGoogleAccount(true, email)
                    _backupEvent.emit(BackupEvent.Success("Connected to Google Drive & Sheets!"))
                } else {
                    _backupEvent.emit(BackupEvent.Error("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."))
                }
            }.onFailure { e ->
                val msg = e.message ?: "Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."
                _backupEvent.emit(BackupEvent.Error(msg))
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            syncRepository.updateGoogleAccount(false, null)
            _backupEvent.emit(BackupEvent.Success("Signed out of Google."))
        }
    }

    fun updateIncludeNotesInSheet(include: Boolean) {
        viewModelScope.launch {
            syncRepository.updateIncludeNotesInSheet(include)
        }
    }

    fun updateAutoSyncEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            syncRepository.updateAutoSyncEnabled(enabled)
            if (enabled) {
                SyncScheduler.scheduleDailyAutoSync(context)
            } else {
                SyncScheduler.cancelAutoSync(context)
            }
        }
    }

    fun syncNow() {
        if (!isSyncSupported) {
            viewModelScope.launch {
                _backupEvent.emit(BackupEvent.Error("Sheets sync is disabled in this flavor."))
            }
            return
        }

        viewModelScope.launch {
            val currentSyncSettings = syncSettings.value
            if (!currentSyncSettings.isGoogleSignedIn) {
                _backupEvent.emit(BackupEvent.Error("Please connect your Google Account first."))
                return@launch
            }

            _isSyncing.value = true
            try {
                val tokenResult = googleAuthManager.getAccessToken()
                if (tokenResult.isFailure) {
                    val err = tokenResult.exceptionOrNull()?.message ?: "Could not retrieve access token."
                    val isRevoked = tokenResult.exceptionOrNull() is SecurityException
                    syncRepository.recordSyncResult(SyncResult.Failure(err, isAuthRevoked = isRevoked))
                    _backupEvent.emit(BackupEvent.Error(err))
                    return@launch
                }

                val accessToken = tokenResult.getOrThrow()
                val properties = propertyRepository.getAllProperties().first()
                val tenants = tenantRepository.getAllTenants().first()
                val payments = paymentRepository.getAllPayments().first()
                val appSettings = settings.value

                val snapshot = SyncSnapshot(
                    properties = properties,
                    tenants = tenants,
                    payments = payments,
                    includeNotes = currentSyncSettings.includeNotesInSheet,
                    currencySymbol = appSettings.currencySymbol
                )

                val result = remoteMirror.sync(snapshot, accessToken)
                syncRepository.recordSyncResult(result)

                when (result) {
                    is SyncResult.Success -> {
                        _backupEvent.emit(BackupEvent.Success("Synced ${result.itemsCount} records to Google Sheet!"))
                    }
                    is SyncResult.Failure -> {
                        _backupEvent.emit(BackupEvent.Error(result.error))
                    }
                }
            } catch (e: Exception) {
                _backupEvent.emit(BackupEvent.Error("Sync error: ${e.message}"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            tenantRepository: TenantRepository,
            propertyRepository: PropertyRepository,
            paymentRepository: PaymentRepository,
            syncRepository: SyncRepository,
            remoteMirror: RemoteMirror,
            googleAuthManager: GoogleAuthManager,
            backupExporter: BackupExporter,
            backupImporter: BackupImporter,
            isSyncSupported: Boolean = BuildConfig.SYNC_ENABLED
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    settingsRepository,
                    tenantRepository,
                    propertyRepository,
                    paymentRepository,
                    syncRepository,
                    remoteMirror,
                    googleAuthManager,
                    backupExporter,
                    backupImporter,
                    isSyncSupported
                ) as T
            }
        }
    }
}
