package com.tenantmanager.ui.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class AddEditTenantFormState(
    val name: String = "",
    val nameError: String? = null,
    val propertyId: String? = null,
    val availableProperties: List<Property> = emptyList(),
    val unitName: String = "",
    val unitNameError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val email: String = "",
    val rentRupees: String = "",
    val rentError: String? = null,
    val dueDay: Int = 5,
    val joinDate: LocalDate = LocalDate.now(),
    val leaveDate: LocalDate? = null,
    val leaveDateError: String? = null,
    val depositRupees: String = "",
    val notes: String = "",
    val isEditing: Boolean = false,
    val isArchived: Boolean = false
)

class AddEditTenantViewModel(
    private val tenantId: String?,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEditTenantFormState())
    val formState: StateFlow<AddEditTenantFormState> = _formState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Boolean>()
    val saveEvent: SharedFlow<Boolean> = _saveEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            val properties = propertyRepository.getAllProperties().first()
            _formState.value = _formState.value.copy(availableProperties = properties)

            if (tenantId != null) {
                val tenant = tenantRepository.getTenantById(tenantId)
                if (tenant != null) {
                    _formState.value = _formState.value.copy(
                        name = tenant.name,
                        propertyId = tenant.propertyId,
                        unitName = tenant.unitName,
                        phone = tenant.phone,
                        email = tenant.email ?: "",
                        rentRupees = (tenant.monthlyRent.minorUnits / 100).toString(),
                        dueDay = tenant.dueDay,
                        joinDate = tenant.joinDate,
                        leaveDate = tenant.leaveDate,
                        depositRupees = if (tenant.securityDeposit.isPositive) (tenant.securityDeposit.minorUnits / 100).toString() else "",
                        notes = tenant.notes ?: "",
                        isEditing = true,
                        isArchived = tenant.isArchived
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _formState.value = _formState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }

    fun onPropertySelected(propertyId: String?) {
        _formState.value = _formState.value.copy(propertyId = propertyId)
    }

    fun onUnitNameChanged(unit: String) {
        _formState.value = _formState.value.copy(
            unitName = unit,
            unitNameError = if (unit.isBlank()) "Unit is required" else null
        )
    }

    fun onPhoneChanged(phone: String) {
        _formState.value = _formState.value.copy(
            phone = phone,
            phoneError = if (phone.isBlank()) "Phone is required" else null
        )
    }

    fun onEmailChanged(email: String) {
        _formState.value = _formState.value.copy(email = email)
    }

    fun onRentChanged(rent: String) {
        val parsed = rent.toLongOrNull()
        _formState.value = _formState.value.copy(
            rentRupees = rent,
            rentError = if (parsed == null || parsed <= 0) "Rent must be greater than 0" else null
        )
    }

    fun onDueDayChanged(day: Int) {
        _formState.value = _formState.value.copy(dueDay = day.coerceIn(1, 31))
    }

    fun onJoinDateChanged(date: LocalDate) {
        val currentLeave = _formState.value.leaveDate
        val leaveError = if (currentLeave != null && !currentLeave.isAfter(date)) {
            "Leave date must be after join date"
        } else null

        _formState.value = _formState.value.copy(
            joinDate = date,
            leaveDateError = leaveError
        )
    }

    fun onLeaveDateChanged(date: LocalDate?) {
        val leaveError = if (date != null && !date.isAfter(_formState.value.joinDate)) {
            "Leave date must be after join date"
        } else null

        _formState.value = _formState.value.copy(
            leaveDate = date,
            leaveDateError = leaveError
        )
    }

    fun onDepositChanged(deposit: String) {
        _formState.value = _formState.value.copy(depositRupees = deposit)
    }

    fun onNotesChanged(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun saveTenant() {
        val state = _formState.value
        val nameErr = if (state.name.isBlank()) "Name is required" else null
        val unitErr = if (state.unitName.isBlank()) "Unit is required" else null
        val phoneErr = if (state.phone.isBlank()) "Phone is required" else null
        val rentVal = state.rentRupees.toLongOrNull()
        val rentErr = if (rentVal == null || rentVal <= 0) "Rent must be greater than 0" else null
        val leaveErr = if (state.leaveDate != null && !state.leaveDate.isAfter(state.joinDate)) {
            "Leave date must be after join date"
        } else null

        if (nameErr != null || unitErr != null || phoneErr != null || rentErr != null || leaveErr != null) {
            _formState.value = state.copy(
                nameError = nameErr,
                unitNameError = unitErr,
                phoneError = phoneErr,
                rentError = rentErr,
                leaveDateError = leaveErr
            )
            return
        }

        viewModelScope.launch {
            val depositVal = state.depositRupees.toLongOrNull() ?: 0L
            val tenant = Tenant(
                id = tenantId ?: UUID.randomUUID().toString(),
                propertyId = state.propertyId,
                name = state.name.trim(),
                phone = state.phone.trim(),
                email = state.email.trim().ifBlank { null },
                unitName = state.unitName.trim(),
                joinDate = state.joinDate,
                leaveDate = state.leaveDate,
                monthlyRent = Money.ofRupees(rentVal!!),
                dueDay = state.dueDay,
                securityDeposit = Money.ofRupees(depositVal),
                notes = state.notes.trim().ifBlank { null },
                isArchived = state.isArchived,
                lastModified = System.currentTimeMillis()
            )

            if (state.isEditing) {
                tenantRepository.updateTenant(tenant)
            } else {
                tenantRepository.insertTenant(tenant)
            }

            _saveEvent.emit(true)
        }
    }

    companion object {
        fun provideFactory(
            tenantId: String?,
            tenantRepository: TenantRepository,
            propertyRepository: PropertyRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddEditTenantViewModel(tenantId, tenantRepository, propertyRepository) as T
            }
        }
    }
}

