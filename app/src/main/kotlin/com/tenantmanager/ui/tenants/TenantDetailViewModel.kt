package com.tenantmanager.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TenantDetailUiState(
    val ledger: TenantLedger? = null,
    val property: Property? = null,
    val payments: List<Payment> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

class TenantDetailViewModel(
    private val tenantId: String,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository,
    private val paymentRepository: PaymentRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<TenantDetailUiState> = combine(
        tenantRepository.getAllTenants(),
        propertyRepository.getAllProperties(),
        paymentRepository.getPaymentsForTenant(tenantId),
        settingsRepository.getSettings()
    ) { tenants, properties, payments, settings ->
        val tenant = tenants.firstOrNull { it.id == tenantId }
        if (tenant == null) {
            TenantDetailUiState(isLoading = false)
        } else {
            val property = tenant.propertyId?.let { pId -> properties.firstOrNull { it.id == pId } }
            val ledger = RentCalculator.calculateLedger(
                tenant = tenant,
                payments = payments,
                asOfDate = LocalDate.now(),
                graceDays = settings.graceDays
            )
            TenantDetailUiState(
                ledger = ledger,
                property = property,
                payments = payments.sortedByDescending { it.paidDate },
                settings = settings,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantDetailUiState()
    )

    fun markAsLeft(leaveDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            tenantRepository.archiveTenant(tenantId, leaveDate.toString())
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            paymentRepository.deletePayment(paymentId)
        }
    }

    companion object {
        fun provideFactory(
            tenantId: String,
            tenantRepository: TenantRepository,
            propertyRepository: PropertyRepository,
            paymentRepository: PaymentRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TenantDetailViewModel(
                    tenantId,
                    tenantRepository,
                    propertyRepository,
                    paymentRepository,
                    settingsRepository
                ) as T
            }
        }
    }
}

