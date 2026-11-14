package com.tenantmanager.domain.repository

import com.tenantmanager.domain.model.Property
import kotlinx.coroutines.flow.Flow

interface PropertyRepository {
    fun getAllProperties(): Flow<List<Property>>
    suspend fun getPropertyById(id: String): Property?
    suspend fun insertProperty(property: Property)
    suspend fun updateProperty(property: Property)
    suspend fun deleteProperty(id: String)
}

