package com.tenantmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String? = null,
    val defaultRentMinor: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

