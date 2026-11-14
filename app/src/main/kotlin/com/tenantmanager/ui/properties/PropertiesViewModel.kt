package com.tenantmanager.ui.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PropertyWithUnitCount(
    val property: Property,
    val unitCount: Int
)

data class PropertiesUiState(
    val properties: List<PropertyWithUnitCount> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

class PropertiesViewModel(
    private val propertyRepository: PropertyRepository,
    tenantRepository: TenantRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<PropertiesUiState> = combine(
        propertyRepository.getAllProperties(),
        tenantRepository.getAllTenants(),
        settingsRepository.getSettings()
    ) { properties, tenants, settings ->
        val counts = tenants.groupBy { it.propertyId }
        val list = properties.map { property ->
            PropertyWithUnitCount(
                property = property,
                unitCount = counts[property.id]?.size ?: 0
            )
        }
        PropertiesUiState(
            properties = list,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PropertiesUiState()
    )

    fun addOrUpdateProperty(id: String?, name: String, address: String?, defaultRentRupees: Long) {
        viewModelScope.launch {
            val property = Property(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                address = address?.trim()?.ifBlank { null },
                defaultRent = Money.ofRupees(defaultRentRupees)
            )
            if (id != null) {
                propertyRepository.updateProperty(property)
            } else {
                propertyRepository.insertProperty(property)
            }
        }
    }

    fun deleteProperty(id: String) {
        viewModelScope.launch {
            propertyRepository.deleteProperty(id)
        }
    }

    companion object {
        fun provideFactory(
            propertyRepository: PropertyRepository,
            tenantRepository: TenantRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PropertiesViewModel(propertyRepository, tenantRepository, settingsRepository) as T
            }
        }
    }
}

