package com.tenantmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alert_history",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tenantId", "periodMonth", "alertType"], unique = true)
    ]
)
data class AlertHistoryEntity(
    @PrimaryKey
    val id: String,
    val tenantId: String,
    val periodMonth: String, // YYYY-MM
    val alertType: String, // DUE_SOON, OVERDUE
    val createdAt: Long = System.currentTimeMillis()
)

