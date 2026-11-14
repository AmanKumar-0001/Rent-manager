package com.tenantmanager.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TenantFilter {
    ALL,
    OVERDUE,
    DUE_SOON,
    PAID,
    LEFT
}

data class TenantsUiState(
    val ledgers: List<TenantLedger> = emptyList(),
    val propertyNames: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilter: TenantFilter = TenantFilter.ALL,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

private data class TenantRepoData(
    val tenants: List<Tenant>,
    val properties: List<Property>,
    val payments: List<Payment>,
    val settings: AppSettings
)

class TenantsViewModel(
    private val tenantRepository: TenantRepository,
    propertyRepository: PropertyRepository,
    paymentRepository: PaymentRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(TenantFilter.ALL)

    private val statusPriority = mapOf(
        TenantStatus.OVERDUE to 0,
        TenantStatus.DUE_SOON to 1,
        TenantStatus.UPCOMING to 2,
        TenantStatus.PAID to 3,
        TenantStatus.LEFT to 4
    )

    private val repoDataFlow = combine(
        tenantRepository.getAllTenants(),
        propertyRepository.getAllProperties(),
        paymentRepository.getAllPayments(),
        settingsRepository.getSettings()
    ) { tenants, properties, payments, settings ->
        TenantRepoData(tenants, properties, payments, settings)
    }

    val uiState: StateFlow<TenantsUiState> = combine(
        repoDataFlow,
        _searchQuery,
        _selectedFilter
    ) { data, query, filter ->
        val today = LocalDate.now()
        val propMap = data.properties.associate { it.id to it.name }

        val allLedgers = data.tenants.map { tenant ->
            RentCalculator.calculateLedger(
                tenant = tenant,
                payments = data.payments,
                asOfDate = today,
                graceDays = data.settings.graceDays
            )
        }

        val filtered = allLedgers.filter { ledger ->
            val matchesQuery = query.isBlank() ||
                    ledger.tenant.name.contains(query, ignoreCase = true) ||
                    ledger.tenant.unitName.contains(query, ignoreCase = true) ||
                    (ledger.tenant.propertyId?.let { propMap[it] }?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                TenantFilter.ALL -> true
                TenantFilter.OVERDUE -> ledger.status == TenantStatus.OVERDUE
                TenantFilter.DUE_SOON -> ledger.status == TenantStatus.DUE_SOON
                TenantFilter.PAID -> ledger.status == TenantStatus.PAID
                TenantFilter.LEFT -> ledger.status == TenantStatus.LEFT || ledger.tenant.isArchived
            }

            matchesQuery && matchesFilter
        }.sortedWith(
            compareBy<TenantLedger> { statusPriority[it.status] ?: 99 }
                .thenBy { it.tenant.name.lowercase() }
        )

        TenantsUiState(
            ledgers = filtered,
            propertyNames = propMap,
            searchQuery = query,
            selectedFilter = filter,
            settings = data.settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: TenantFilter) {
        _selectedFilter.value = filter
    }

    fun archiveTenant(tenantId: String) {
        viewModelScope.launch {
            tenantRepository.archiveTenant(tenantId, LocalDate.now().toString())
        }
    }

    companion object {
        fun provideFactory(
            tenantRepository: TenantRepository,
            propertyRepository: PropertyRepository,
            paymentRepository: PaymentRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TenantsViewModel(tenantRepository, propertyRepository, paymentRepository, settingsRepository) as T
            }
        }
    }
}
