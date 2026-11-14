package com.tenantmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["periodMonth"])
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val tenantId: String,
    val amountMinor: Long,
    val paidDate: String, // YYYY-MM-DD
    val periodMonth: String, // YYYY-MM
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

