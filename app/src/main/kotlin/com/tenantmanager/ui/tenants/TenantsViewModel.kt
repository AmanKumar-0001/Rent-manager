package com.tenantmanager.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class TenantFilter {
    ALL,
    ACTIVE,
    OVERDUE,
    DUE_SOON,
    LEFT
}

data class TenantsUiState(
    val ledgers: List<TenantLedger> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TenantFilter = TenantFilter.ALL,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

class TenantsViewModel(
    private val tenantRepository: TenantRepository,
    paymentRepository: PaymentRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(TenantFilter.ALL)

    val uiState: StateFlow<TenantsUiState> = combine(
        tenantRepository.getAllTenants(),
        paymentRepository.getAllPayments(),
        settingsRepository.getSettings(),
        _searchQuery,
        _selectedFilter
    ) { tenants, payments, settings, query, filter ->
        val today = LocalDate.now()
        val allLedgers = tenants.map { tenant ->
            RentCalculator.calculateLedger(
                tenant = tenant,
                payments = payments,
                asOfDate = today,
                graceDays = settings.graceDays
            )
        }

        val filtered = allLedgers.filter { ledger ->
            val matchesQuery = query.isBlank() ||
                    ledger.tenant.name.contains(query, ignoreCase = true) ||
                    ledger.tenant.unitName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                TenantFilter.ALL -> true
                TenantFilter.ACTIVE -> !ledger.tenant.isArchived && ledger.status != TenantStatus.LEFT
                TenantFilter.OVERDUE -> ledger.status == TenantStatus.OVERDUE
                TenantFilter.DUE_SOON -> ledger.status == TenantStatus.DUE_SOON
                TenantFilter.LEFT -> ledger.status == TenantStatus.LEFT || ledger.tenant.isArchived
            }

            matchesQuery && matchesFilter
        }

        TenantsUiState(
            ledgers = filtered,
            searchQuery = query,
            selectedFilter = filter,
            settings = settings,
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

    companion object {
        fun provideFactory(
            tenantRepository: TenantRepository,
            paymentRepository: PaymentRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TenantsViewModel(tenantRepository, paymentRepository, settingsRepository) as T
            }
        }
    }
}

