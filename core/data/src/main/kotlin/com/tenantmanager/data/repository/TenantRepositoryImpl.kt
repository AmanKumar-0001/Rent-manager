package com.tenantmanager.data.repository

import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.local.dao.TenantDao
import com.tenantmanager.data.mapper.toDomain
import com.tenantmanager.data.mapper.toEntity
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TenantRepositoryImpl(
    private val database: AppDatabase,
    private val tenantDao: TenantDao
) : TenantRepository {

    override fun getAllTenants(): Flow<List<Tenant>> {
        return tenantDao.getAllTenants().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveTenants(): Flow<List<Tenant>> {
        return tenantDao.getActiveTenants().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTenantById(id: String): Tenant? {
        return tenantDao.getTenantById(id)?.toDomain()
    }

    override suspend fun insertTenant(tenant: Tenant) {
        tenantDao.insertTenant(tenant.toEntity())
    }

    override suspend fun updateTenant(tenant: Tenant) {
        tenantDao.updateTenant(tenant.toEntity())
    }

    override suspend fun archiveTenant(id: String, leaveDate: String) {
        tenantDao.archiveTenant(id, leaveDate)
    }

    override suspend fun deleteTenant(id: String) {
        tenantDao.deleteTenant(id)
    }

    override suspend fun wipeAllData() {
        database.clearAllTables()
    }
}

