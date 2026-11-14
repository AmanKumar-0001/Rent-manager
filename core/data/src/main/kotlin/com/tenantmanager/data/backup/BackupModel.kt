package com.tenantmanager.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val properties: List<PropertyBackupDto> = emptyList(),
    val tenants: List<TenantBackupDto> = emptyList(),
    val payments: List<PaymentBackupDto> = emptyList()
)

@Serializable
data class PropertyBackupDto(
    val id: String,
    val name: String,
    val address: String? = null,
    val defaultRentMinor: Long = 0L,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L
)

@Serializable
data class TenantBackupDto(
    val id: String,
    val propertyId: String? = null,
    val name: String,
    val phone: String,
    val email: String? = null,
    val unitName: String,
    val joinDate: String,
    val leaveDate: String? = null,
    val monthlyRentMinor: Long,
    val dueDay: Int,
    val securityDepositMinor: Long = 0L,
    val depositReturnedDate: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L
)

@Serializable
data class PaymentBackupDto(
    val id: String,
    val tenantId: String,
    val amountMinor: Long,
    val paidDate: String,
    val periodMonth: String,
    val notes: String? = null,
    val createdAt: Long = 0L,
    val lastModified: Long = 0L
)

