package com.tenantmanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.calculator.RentCalculator
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.PortfolioSummary
import com.tenantmanager.domain.model.TenantLedger
import com.tenantmanager.domain.model.TenantStatus
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUiState(
    val summary: PortfolioSummary = PortfolioSummary(0, Money.ZERO, Money.ZERO, Money.ZERO),
    val priorityTenants: List<TenantLedger> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    tenantRepository: TenantRepository,
    paymentRepository: PaymentRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        tenantRepository.getAllTenants(),
        paymentRepository.getAllPayments(),
        settingsRepository.getSettings()
    ) { tenants, payments, settings ->
        val today = LocalDate.now()
        val ledgers = tenants.map { tenant ->
            RentCalculator.calculateLedger(
                tenant = tenant,
                payments = payments,
                asOfDate = today,
                graceDays = settings.graceDays
            )
        }

        val summary = RentCalculator.calculatePortfolioSummary(
            tenants = tenants,
            ledgers = ledgers,
            asOfDate = today
        )

        val priority = ledgers.filter {
            it.status == TenantStatus.OVERDUE || it.status == TenantStatus.DUE_SOON
        }.sortedBy { it.nextPaymentDate ?: LocalDate.MAX }

        DashboardUiState(
            summary = summary,
            priorityTenants = priority,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    companion object {
        fun provideFactory(
            tenantRepository: TenantRepository,
            paymentRepository: PaymentRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(tenantRepository, paymentRepository, settingsRepository) as T
            }
        }
    }
}

