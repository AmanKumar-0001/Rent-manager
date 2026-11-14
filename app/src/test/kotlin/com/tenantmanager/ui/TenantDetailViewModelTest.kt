package com.tenantmanager.ui

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.ui.tenants.TenantDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TenantDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tenantsFlow = MutableStateFlow<List<Tenant>>(emptyList())
    private val propertiesFlow = MutableStateFlow<List<Property>>(emptyList())
    private val paymentsFlow = MutableStateFlow<List<Payment>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    private val fakeTenantRepository = object : TenantRepository {
        override fun getAllTenants(): Flow<List<Tenant>> = tenantsFlow.asStateFlow()
        override fun getActiveTenants(): Flow<List<Tenant>> = tenantsFlow.asStateFlow()
        override suspend fun getTenantById(id: String): Tenant? = tenantsFlow.value.firstOrNull { it.id == id }
        override suspend fun insertTenant(tenant: Tenant) {}
        override suspend fun updateTenant(tenant: Tenant) {}
        override suspend fun archiveTenant(id: String, leaveDate: String) {
            tenantsFlow.value = tenantsFlow.value.map {
                if (it.id == id) it.copy(isArchived = true, leaveDate = LocalDate.parse(leaveDate)) else it
            }
        }
        override suspend fun deleteTenant(id: String) {}
        override suspend fun wipeAllData() {}
    }

    private val fakePropertyRepository = object : PropertyRepository {
        override fun getAllProperties(): Flow<List<Property>> = propertiesFlow.asStateFlow()
        override suspend fun getPropertyById(id: String): Property? = null
        override suspend fun insertProperty(property: Property) {}
        override suspend fun updateProperty(property: Property) {}
        override suspend fun deleteProperty(id: String) {}
    }

    private val fakePaymentRepository = object : PaymentRepository {
        override fun getAllPayments(): Flow<List<Payment>> = paymentsFlow.asStateFlow()
        override fun getPaymentsForTenant(tenantId: String): Flow<List<Payment>> = paymentsFlow.asStateFlow()
        override suspend fun getPaymentById(id: String): Payment? = paymentsFlow.value.firstOrNull { it.id == id }
        override suspend fun insertPayment(payment: Payment) {
            paymentsFlow.value = paymentsFlow.value + payment
        }
        override suspend fun deletePayment(id: String) {}
    }

    private val fakeSettingsRepository = object : SettingsRepository {
        override fun getSettings(): Flow<AppSettings> = settingsFlow.asStateFlow()
        override suspend fun updateCurrencySymbol(symbol: String) {}
        override suspend fun updateGraceDays(days: Int) {}
        override suspend fun updateReminderHour(hour: Int) {}
        override suspend fun updateAppLockEnabled(enabled: Boolean) {}
        override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {}
        override suspend fun updateOnboardingCompleted(completed: Boolean) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ledger calculates status and markAsLeft archives tenant`() = runTest {
        val tenant = Tenant(
            id = "t1",
            name = "Rohan Verma",
            phone = "9876543210",
            unitName = "B-202",
            joinDate = LocalDate.now().minusMonths(2),
            monthlyRent = Money.ofRupees(20000),
            dueDay = 1
        )
        tenantsFlow.value = listOf(tenant)

        val viewModel = TenantDetailViewModel(
            tenantId = "t1",
            tenantRepository = fakeTenantRepository,
            propertyRepository = fakePropertyRepository,
            paymentRepository = fakePaymentRepository,
            settingsRepository = fakeSettingsRepository
        )

        val collectJob = launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.ledger).isNotNull()
        assertThat(viewModel.uiState.value.ledger?.tenant?.name).isEqualTo("Rohan Verma")
        assertThat(viewModel.uiState.value.ledger?.status).isEqualTo(TenantStatus.OVERDUE)

        // Mark as left
        val leaveDate = LocalDate.now().minusDays(1)
        viewModel.markAsLeft(leaveDate)
        advanceUntilIdle()

        // Tenant is now archived and status is LEFT
        val updatedTenant = tenantsFlow.value.first { it.id == "t1" }
        assertThat(updatedTenant.isArchived).isTrue()
        assertThat(updatedTenant.leaveDate).isEqualTo(leaveDate)
        assertThat(viewModel.uiState.value.ledger?.status).isEqualTo(TenantStatus.LEFT)

        collectJob.cancel()
    }

    @Test
    fun `recording a payment recalculates the tenant ledger and status`() = runTest {
        val today = LocalDate.now()
        val tenant = Tenant(
            id = "t2",
            name = "Priya Rao",
            phone = "9811122233",
            unitName = "104",
            joinDate = today,
            monthlyRent = Money.ofRupees(12000),
            dueDay = today.dayOfMonth
        )
        tenantsFlow.value = listOf(tenant)

        val viewModel = TenantDetailViewModel(
            tenantId = "t2",
            tenantRepository = fakeTenantRepository,
            propertyRepository = fakePropertyRepository,
            paymentRepository = fakePaymentRepository,
            settingsRepository = fakeSettingsRepository
        )

        val collectJob = launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.ledger?.totalBalance).isEqualTo(Money.ofRupees(12000))

        // Record full payment
        fakePaymentRepository.insertPayment(
            Payment(
                id = "p1",
                tenantId = "t2",
                amount = Money.ofRupees(12000),
                paidDate = today,
                periodMonth = YearMonth.from(today)
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.ledger?.status).isEqualTo(TenantStatus.PAID)
        assertThat(viewModel.uiState.value.ledger?.totalBalance).isEqualTo(Money.ZERO)

        collectJob.cancel()
    }
}

