package com.tenantmanager.ui

import com.google.common.truth.Truth.assertThat
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.ui.tenants.AddEditTenantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTenantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeTenants = mutableListOf<Tenant>()
    private val tenantsFlow = MutableStateFlow<List<Tenant>>(emptyList())
    private val propertiesFlow = MutableStateFlow<List<Property>>(emptyList())

    private val fakeTenantRepository = object : TenantRepository {
        override fun getAllTenants(): Flow<List<Tenant>> = tenantsFlow.asStateFlow()
        override fun getActiveTenants(): Flow<List<Tenant>> = tenantsFlow.asStateFlow()
        override suspend fun getTenantById(id: String): Tenant? = fakeTenants.firstOrNull { it.id == id }
        override suspend fun insertTenant(tenant: Tenant) {
            fakeTenants.add(tenant)
            tenantsFlow.value = fakeTenants.toList()
        }
        override suspend fun updateTenant(tenant: Tenant) {
            val idx = fakeTenants.indexOfFirst { it.id == tenant.id }
            if (idx >= 0) fakeTenants[idx] = tenant
            tenantsFlow.value = fakeTenants.toList()
        }
        override suspend fun archiveTenant(id: String, leaveDate: String) {
            val idx = fakeTenants.indexOfFirst { it.id == id }
            if (idx >= 0) {
                fakeTenants[idx] = fakeTenants[idx].copy(isArchived = true, leaveDate = LocalDate.parse(leaveDate))
                tenantsFlow.value = fakeTenants.toList()
            }
        }
        override suspend fun deleteTenant(id: String) {
            fakeTenants.removeAll { it.id == id }
            tenantsFlow.value = fakeTenants.toList()
        }
        override suspend fun wipeAllData() {
            fakeTenants.clear()
            tenantsFlow.value = emptyList()
        }
    }

    private val fakePropertyRepository = object : PropertyRepository {
        override fun getAllProperties(): Flow<List<Property>> = propertiesFlow.asStateFlow()
        override suspend fun getPropertyById(id: String): Property? = null
        override suspend fun insertProperty(property: Property) {}
        override suspend fun updateProperty(property: Property) {}
        override suspend fun deleteProperty(id: String) {}
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
    fun `validates rent greater than 0 and dueDay in 1 to 31`() = runTest {
        val viewModel = AddEditTenantViewModel(
            tenantId = null,
            tenantRepository = fakeTenantRepository,
            propertyRepository = fakePropertyRepository
        )
        advanceUntilIdle()

        // 1. Invalid rent: 0
        viewModel.onRentChanged("0")
        assertThat(viewModel.formState.value.rentError).isEqualTo("Rent must be greater than 0")

        // 2. Negative rent
        viewModel.onRentChanged("-500")
        assertThat(viewModel.formState.value.rentError).isEqualTo("Rent must be greater than 0")

        // 3. Valid rent
        viewModel.onRentChanged("15000")
        assertThat(viewModel.formState.value.rentError).isNull()

        // 4. Clamping dueDay outside 1..31
        viewModel.onDueDayChanged(0)
        assertThat(viewModel.formState.value.dueDay).isEqualTo(1)

        viewModel.onDueDayChanged(35)
        assertThat(viewModel.formState.value.dueDay).isEqualTo(31)

        viewModel.onDueDayChanged(15)
        assertThat(viewModel.formState.value.dueDay).isEqualTo(15)

        // 5. Validation stops saving if name is blank
        viewModel.onNameChanged("")
        viewModel.onUnitNameChanged("101")
        viewModel.onPhoneChanged("9999999999")
        viewModel.saveTenant()
        advanceUntilIdle()

        assertThat(viewModel.formState.value.nameError).isEqualTo("Name is required")
        assertThat(fakeTenants).isEmpty()

        // 6. Valid save
        viewModel.onNameChanged("Vikram Malhotra")
        viewModel.saveTenant()
        advanceUntilIdle()

        assertThat(fakeTenants).hasSize(1)
        assertThat(fakeTenants[0].name).isEqualTo("Vikram Malhotra")
        assertThat(fakeTenants[0].monthlyRent).isEqualTo(Money.ofRupees(15000))
        assertThat(fakeTenants[0].dueDay).isEqualTo(15)
    }
}

