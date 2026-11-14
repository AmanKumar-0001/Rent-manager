package com.tenantmanager.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.tenantmanager.auth.GoogleAuthManager
import com.tenantmanager.data.backup.BackupExporter
import com.tenantmanager.data.backup.BackupImporter
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.repository.PaymentRepositoryImpl
import com.tenantmanager.data.repository.PropertyRepositoryImpl
import com.tenantmanager.data.repository.SettingsRepositoryImpl
import com.tenantmanager.data.repository.SyncRepositoryImpl
import com.tenantmanager.data.repository.TenantRepositoryImpl
import com.tenantmanager.data.sync.FakeRemoteMirror
import com.tenantmanager.di.AppContainer
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.SheetsSyncSettings
import com.tenantmanager.domain.repository.SyncRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.domain.sync.RemoteMirror
import com.tenantmanager.domain.sync.SheetRowMapper
import com.tenantmanager.domain.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SheetsSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var fakeMirror: FakeRemoteMirror
    private lateinit var testContainer: AppContainer

    private val syncSettingsFlow = MutableStateFlow(
        SheetsSyncSettings(
            isGoogleSignedIn = true,
            userEmail = "landlord@example.com",
            autoSyncEnabled = true,
            includeNotesInSheet = false
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeMirror = FakeRemoteMirror()

        val propRepo = PropertyRepositoryImpl(database.propertyDao())
        val tenantRepo = TenantRepositoryImpl(database, database.tenantDao())
        val paymentRepo = PaymentRepositoryImpl(database.paymentDao())
        val exporter = BackupExporter(database)
        val importer = BackupImporter(database)

        val settingsRepo = object : SettingsRepository {
            override fun getSettings(): Flow<AppSettings> = MutableStateFlow(AppSettings()).asStateFlow()
            override suspend fun updateCurrencySymbol(symbol: String) {}
            override suspend fun updateGraceDays(days: Int) {}
            override suspend fun updateReminderHour(hour: Int) {}
            override suspend fun updateAppLockEnabled(enabled: Boolean) {}
        }

        val syncRepo = object : SyncRepository {
            override fun getSyncSettings(): Flow<SheetsSyncSettings> = syncSettingsFlow.asStateFlow()
            override suspend fun updateGoogleAccount(isSignedIn: Boolean, email: String?) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(isGoogleSignedIn = isSignedIn, userEmail = email)
            }
            override suspend fun updateIncludeNotesInSheet(include: Boolean) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(includeNotesInSheet = include)
            }
            override suspend fun updateAutoSyncEnabled(enabled: Boolean) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(autoSyncEnabled = enabled)
            }
            override suspend fun updateSpreadsheetInfo(id: String, name: String) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(spreadsheetId = id, spreadsheetName = name)
            }
            override suspend fun recordSyncResult(result: SyncResult) {
                when (result) {
                    is SyncResult.Success -> {
                        syncSettingsFlow.value = syncSettingsFlow.value.copy(
                            lastSyncTimestamp = result.timestamp,
                            lastSyncStatus = "Success (${result.itemsCount} items)",
                            spreadsheetId = result.spreadsheetId
                        )
                    }
                    is SyncResult.Failure -> {
                        syncSettingsFlow.value = syncSettingsFlow.value.copy(
                            lastSyncTimestamp = result.timestamp,
                            lastSyncStatus = "Failed: ${result.error}",
                            isGoogleSignedIn = if (result.isAuthRevoked) false else syncSettingsFlow.value.isGoogleSignedIn
                        )
                    }
                }
            }
        }

        val authManager = object : GoogleAuthManager(context) {
            // Can override or simulate token responses
        }

        testContainer = object : AppContainer {
            override val database: AppDatabase = this@SheetsSyncWorkerTest.database
            override val propertyRepository: PropertyRepository = propRepo
            override val tenantRepository: TenantRepository = tenantRepo
            override val paymentRepository: PaymentRepository = paymentRepo
            override val settingsRepository: SettingsRepository = settingsRepo
            override val syncRepository: SyncRepository = syncRepo
            override val remoteMirror: RemoteMirror = fakeMirror
            override val googleAuthManager: GoogleAuthManager = authManager
            override val backupExporter: BackupExporter = exporter
            override val backupImporter: BackupImporter = importer
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `sync disabled when user is not signed in returns success without uploading`() = runBlocking {
        syncSettingsFlow.value = syncSettingsFlow.value.copy(isGoogleSignedIn = false)

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return SheetsSyncWorker(appContext, workerParameters, testContainer)
            }
        }

        val worker = TestListenableWorkerBuilder<SheetsSyncWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(fakeMirror.fakeApiClient.batchUpdateCallsCount).isEqualTo(0)
    }

    @Test
    fun `sync pushes snapshot with all 3 tabs when signed in`() = runBlocking {
        // Insert a property and a tenant
        testContainer.propertyRepository.insertProperty(
            Property("p1", "Galaxy Apartments", "Main Road", Money.ofRupees(30000))
        )
        testContainer.tenantRepository.insertTenant(
            Tenant(
                id = "t1",
                propertyId = "p1",
                name = "Neha Gupta",
                phone = "9876543210",
                unitName = "A-303",
                joinDate = LocalDate.now(),
                monthlyRent = Money.ofRupees(30000),
                dueDay = 1
            )
        )

        // Directly test sync via remote mirror
        val snapshot = com.tenantmanager.domain.sync.SyncSnapshot(
            properties = testContainer.propertyRepository.getAllProperties().first(),
            tenants = testContainer.tenantRepository.getAllTenants().first(),
            payments = emptyList(),
            includeNotes = false
        )

        val syncResult = testContainer.remoteMirror.sync(snapshot, "test-valid-token")
        testContainer.syncRepository.recordSyncResult(syncResult)

        assertThat(syncResult).isInstanceOf(SyncResult.Success::class.java)
        assertThat(fakeMirror.fakeApiClient.batchUpdateCallsCount).isEqualTo(1)
        assertThat(fakeMirror.fakeApiClient.uploadedTabs.keys).containsExactly(
            SheetRowMapper.TAB_TENANTS,
            SheetRowMapper.TAB_PAYMENTS,
            SheetRowMapper.TAB_PROPERTIES
        )

        val status = testContainer.syncRepository.getSyncSettings().first().lastSyncStatus
        assertThat(status).contains("Success")
    }
}

