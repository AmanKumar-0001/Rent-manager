package com.tenantmanager.domain.model

import java.time.LocalDate

data class Tenant(
    val id: String,
    val propertyId: String? = null,
    val name: String,
    val phone: String,
    val email: String? = null,
    val unitName: String,
    val joinDate: LocalDate,
    val leaveDate: LocalDate? = null,
    val monthlyRent: Money,
    val dueDay: Int, // 1..31, where 31 means last day of month
    val securityDeposit: Money = Money.ZERO,
    val depositReturnedDate: LocalDate? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val lastModified: Long = 0L
)

