package com.tenantmanager.data.mapper

import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity
import com.tenantmanager.domain.model.Money
import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import java.time.LocalDate
import java.time.YearMonth

fun PropertyEntity.toDomain(): Property {
    return Property(
        id = id,
        name = name,
        address = address,
        defaultRent = Money.ofPaise(defaultRentMinor)
    )
}

fun Property.toEntity(): PropertyEntity {
    return PropertyEntity(
        id = id,
        name = name,
        address = address,
        defaultRentMinor = defaultRent.minorUnits
    )
}

fun TenantEntity.toDomain(): Tenant {
    return Tenant(
        id = id,
        propertyId = propertyId,
        name = name,
        phone = phone,
        email = email,
        unitName = unitName,
        joinDate = LocalDate.parse(joinDate),
        leaveDate = leaveDate?.let { LocalDate.parse(it) },
        monthlyRent = Money.ofPaise(monthlyRentMinor),
        dueDay = dueDay,
        securityDeposit = Money.ofPaise(securityDepositMinor),
        depositReturnedDate = depositReturnedDate?.let { LocalDate.parse(it) },
        notes = notes,
        isArchived = isArchived,
        lastModified = lastModified
    )
}

fun Tenant.toEntity(): TenantEntity {
    return TenantEntity(
        id = id,
        propertyId = propertyId,
        name = name,
        phone = phone,
        email = email,
        unitName = unitName,
        joinDate = joinDate.toString(),
        leaveDate = leaveDate?.toString(),
        monthlyRentMinor = monthlyRent.minorUnits,
        dueDay = dueDay,
        securityDepositMinor = securityDeposit.minorUnits,
        depositReturnedDate = depositReturnedDate?.toString(),
        notes = notes,
        isArchived = isArchived,
        lastModified = if (lastModified != 0L) lastModified else System.currentTimeMillis()
    )
}

fun PaymentEntity.toDomain(): Payment {
    return Payment(
        id = id,
        tenantId = tenantId,
        amount = Money.ofPaise(amountMinor),
        paidDate = LocalDate.parse(paidDate),
        periodMonth = YearMonth.parse(periodMonth),
        notes = notes,
        lastModified = lastModified
    )
}

fun Payment.toEntity(): PaymentEntity {
    return PaymentEntity(
        id = id,
        tenantId = tenantId,
        amountMinor = amount.minorUnits,
        paidDate = paidDate.toString(),
        periodMonth = periodMonth.toString(),
        notes = notes,
        lastModified = if (lastModified != 0L) lastModified else System.currentTimeMillis()
    )
}

