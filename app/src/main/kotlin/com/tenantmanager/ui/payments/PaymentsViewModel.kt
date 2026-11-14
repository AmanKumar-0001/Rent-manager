package com.tenantmanager.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaymentWithTenant(
    val payment: Payment,
    val tenantName: String,
    val unitName: String
)

data class PaymentsUiState(
    val payments: List<PaymentWithTenant> = emptyList(),
    val searchQuery: String = "",
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

class PaymentsViewModel(
    private val paymentRepository: PaymentRepository,
    tenantRepository: TenantRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PaymentsUiState> = combine(
        paymentRepository.getAllPayments(),
        tenantRepository.getAllTenants(),
        settingsRepository.getSettings(),
        _searchQuery
    ) { payments, tenants, settings, query ->
        val tenantMap = tenants.associateBy { it.id }
        val mapped = payments.map { payment ->
            val tenant = tenantMap[payment.tenantId]
            PaymentWithTenant(
                payment = payment,
                tenantName = tenant?.name ?: "Unknown Tenant",
                unitName = tenant?.unitName ?: "—"
            )
        }

        val filtered = mapped.filter { item ->
            query.isBlank() ||
                    item.tenantName.contains(query, ignoreCase = true) ||
                    item.unitName.contains(query, ignoreCase = true) ||
                    (item.payment.notes?.contains(query, ignoreCase = true) == true)
        }.sortedByDescending { it.payment.paidDate }

        PaymentsUiState(
            payments = filtered,
            searchQuery = query,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaymentsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            paymentRepository.deletePayment(id)
        }
    }

    companion object {
        fun provideFactory(
            paymentRepository: PaymentRepository,
            tenantRepository: TenantRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaymentsViewModel(paymentRepository, tenantRepository, settingsRepository) as T
            }
        }
    }
}

