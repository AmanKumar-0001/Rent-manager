package com.tenantmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenants",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["propertyId"]),
        Index(value = ["isArchived"])
    ]
)
data class TenantEntity(
    @PrimaryKey
    val id: String,
    val propertyId: String? = null,
    val name: String,
    val phone: String,
    val email: String? = null,
    val unitName: String,
    val joinDate: String, // YYYY-MM-DD
    val leaveDate: String? = null, // YYYY-MM-DD
    val monthlyRentMinor: Long,
    val dueDay: Int, // 1..31
    val securityDepositMinor: Long = 0L,
    val depositReturnedDate: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

