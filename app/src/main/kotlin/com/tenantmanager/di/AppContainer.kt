package com.tenantmanager.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.tenantmanager.BuildConfig
import com.tenantmanager.auth.GoogleAuthManager
import com.tenantmanager.data.backup.BackupExporter
import com.tenantmanager.data.backup.BackupImporter
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.repository.PaymentRepositoryImpl
import com.tenantmanager.data.repository.PropertyRepositoryImpl
import com.tenantmanager.data.repository.SettingsRepositoryImpl
import com.tenantmanager.data.repository.SyncRepositoryImpl
import com.tenantmanager.data.repository.TenantRepositoryImpl
import com.tenantmanager.data.sync.RealSheetsApiClient
import com.tenantmanager.data.sync.SheetsRemoteMirror
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.SyncRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.domain.sync.NoOpRemoteMirror
import com.tenantmanager.domain.sync.RemoteMirror

interface AppContainer {
    val database: AppDatabase
    val propertyRepository: PropertyRepository
    val tenantRepository: TenantRepository
    val paymentRepository: PaymentRepository
    val settingsRepository: SettingsRepository
    val syncRepository: SyncRepository
    val remoteMirror: RemoteMirror
    val googleAuthManager: GoogleAuthManager
    val backupExporter: BackupExporter
    val backupImporter: BackupImporter
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "tenant_manager.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_settings") }
        )
    }

    override val propertyRepository: PropertyRepository by lazy {
        PropertyRepositoryImpl(database.propertyDao())
    }

    override val tenantRepository: TenantRepository by lazy {
        TenantRepositoryImpl(database, database.tenantDao())
    }

    override val paymentRepository: PaymentRepository by lazy {
        PaymentRepositoryImpl(database.paymentDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(dataStore)
    }

    override val syncRepository: SyncRepository by lazy {
        SyncRepositoryImpl(dataStore, database.syncLogDao())
    }

    override val remoteMirror: RemoteMirror by lazy {
        if (BuildConfig.SYNC_ENABLED) {
            SheetsRemoteMirror(RealSheetsApiClient())
        } else {
            NoOpRemoteMirror()
        }
    }

    override val googleAuthManager: GoogleAuthManager by lazy {
        GoogleAuthManager(context)
    }

    override val backupExporter: BackupExporter by lazy {
        BackupExporter(database)
    }

    override val backupImporter: BackupImporter by lazy {
        BackupImporter(database)
    }
}

