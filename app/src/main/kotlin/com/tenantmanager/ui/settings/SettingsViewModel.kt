package com.tenantmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.data.backup.BackupExporter
import com.tenantmanager.data.backup.BackupImporter
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupEvent {
    data class ExportPlaintextReady(val json: String) : BackupEvent()
    data class ExportEncryptedReady(val bytes: ByteArray) : BackupEvent()
    data class Success(val message: String) : BackupEvent()
    data class Error(val message: String) : BackupEvent()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val tenantRepository: TenantRepository,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

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

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            tenantRepository: TenantRepository,
            backupExporter: BackupExporter,
            backupImporter: BackupImporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, tenantRepository, backupExporter, backupImporter) as T
            }
        }
    }
}

