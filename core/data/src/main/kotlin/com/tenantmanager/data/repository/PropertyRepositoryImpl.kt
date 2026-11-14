package com.tenantmanager.data.repository

import com.tenantmanager.data.local.dao.PropertyDao
import com.tenantmanager.data.mapper.toDomain
import com.tenantmanager.data.mapper.toEntity
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.repository.PropertyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PropertyRepositoryImpl(
    private val propertyDao: PropertyDao
) : PropertyRepository {
    override fun getAllProperties(): Flow<List<Property>> {
        return propertyDao.getAllProperties().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPropertyById(id: String): Property? {
        return propertyDao.getPropertyById(id)?.toDomain()
    }

    override suspend fun insertProperty(property: Property) {
        propertyDao.insertProperty(property.toEntity())
    }

    override suspend fun updateProperty(property: Property) {
        propertyDao.updateProperty(property.toEntity())
    }

    override suspend fun deleteProperty(id: String) {
        propertyDao.deleteProperty(id)
    }
}

