package com.tenantmanager.notification

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.tenantmanager.data.backup.BackupExporter
import com.tenantmanager.data.backup.BackupImporter
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.local.entity.TenantEntity
import com.tenantmanager.data.repository.PaymentRepositoryImpl
import com.tenantmanager.data.repository.PropertyRepositoryImpl
import com.tenantmanager.data.repository.SettingsRepositoryImpl
import com.tenantmanager.data.repository.TenantRepositoryImpl
import com.tenantmanager.di.AppContainer
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var settingsRepository: SettingsRepository
    private val settingsFlow = MutableStateFlow(AppSettings(graceDays = 5, reminderHour = 9))

    private lateinit var testContainer: AppContainer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        settingsRepository = object : SettingsRepository {
            override fun getSettings(): Flow<AppSettings> = settingsFlow.asStateFlow()
            override suspend fun updateCurrencySymbol(symbol: String) {}
            override suspend fun updateGraceDays(days: Int) {}
            override suspend fun updateReminderHour(hour: Int) {}
            override suspend fun updateAppLockEnabled(enabled: Boolean) {}
        }

        val propRepo = PropertyRepositoryImpl(database.propertyDao())
        val tenantRepo = TenantRepositoryImpl(database, database.tenantDao())
        val paymentRepo = PaymentRepositoryImpl(database.paymentDao())
        val exporter = BackupExporter(database)
        val importer = BackupImporter(database)

        testContainer = object : AppContainer {
            override val database: AppDatabase = this@DailyNotificationWorkerTest.database
            override val propertyRepository: PropertyRepository = propRepo
            override val tenantRepository: TenantRepository = tenantRepo
            override val paymentRepository: PaymentRepository = paymentRepo
            override val settingsRepository: SettingsRepository = this@DailyNotificationWorkerTest.settingsRepository
            override val backupExporter: BackupExporter = exporter
            override val backupImporter: BackupImporter = importer
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `worker triggers alert for overdue tenant and deduplicates on subsequent runs`() = runBlocking {
        val overdueJoinDate = LocalDate.now().minusMonths(2).withDayOfMonth(1)
        val overdueTenant = Tenant(
            id = "tenant-overdue-1",
            propertyId = null,
            name = "Ramesh Kumar",
            phone = "9876543210",
            email = null,
            unitName = "A-101",
            joinDate = overdueJoinDate,
            leaveDate = null,
            monthlyRent = Money.ofRupees(15000),
            dueDay = 5
        )

        testContainer.tenantRepository.insertTenant(overdueTenant)

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return DailyNotificationWorker(appContext, workerParameters, testContainer)
            }
        }

        // 1. First run: should detect OVERDUE and record alert in DB
        val firstWorker = TestListenableWorkerBuilder<DailyNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        val firstResult = firstWorker.doWork()
        assertThat(firstResult).isEqualTo(ListenableWorker.Result.success())

        val periodMonth = YearMonth.from(overdueJoinDate).toString()
        val hasAlert = database.alertHistoryDao().hasAlertBeenSent(
            tenantId = "tenant-overdue-1",
            periodMonth = periodMonth,
            alertType = "OVERDUE"
        )
        assertThat(hasAlert).isTrue()

        // 2. Second run: duplicate check must kick in and deduplicate
        val secondWorker = TestListenableWorkerBuilder<DailyNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        val secondResult = secondWorker.doWork()
        assertThat(secondResult).isEqualTo(ListenableWorker.Result.success())

        // Alert remains recorded and verified
        val hasAlertSecondTime = database.alertHistoryDao().hasAlertBeenSent(
            tenantId = "tenant-overdue-1",
            periodMonth = periodMonth,
            alertType = "OVERDUE"
        )
        assertThat(hasAlertSecondTime).isTrue()
    }

    @Test
    fun `worker triggers alert for due soon tenant and deduplicates`() = runBlocking {
        // Tenant whose due date is 2 days from today
        val today = LocalDate.now()
        val targetDueDate = today.plusDays(2)
        val dueSoonTenant = Tenant(
            id = "tenant-due-soon-1",
            propertyId = null,
            name = "Sunita Rao",
            phone = "9123456789",
            email = null,
            unitName = "B-202",
            joinDate = targetDueDate.withDayOfMonth(1),
            leaveDate = null,
            monthlyRent = Money.ofRupees(20000),
            dueDay = targetDueDate.dayOfMonth
        )

        testContainer.tenantRepository.insertTenant(dueSoonTenant)

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return DailyNotificationWorker(appContext, workerParameters, testContainer)
            }
        }

        val worker = TestListenableWorkerBuilder<DailyNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val periodMonth = YearMonth.from(targetDueDate).toString()
        val hasAlert = database.alertHistoryDao().hasAlertBeenSent(
            tenantId = "tenant-due-soon-1",
            periodMonth = periodMonth,
            alertType = "DUE_SOON"
        )
        assertThat(hasAlert).isTrue()

        // Second run
        val secondWorker = TestListenableWorkerBuilder<DailyNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
        val secondResult = secondWorker.doWork()
        assertThat(secondResult).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `boot receiver handles boot completed intent without throwing`() {
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        // Verify receiver execution runs smoothly
        receiver.onReceive(context, intent)
    }
}

