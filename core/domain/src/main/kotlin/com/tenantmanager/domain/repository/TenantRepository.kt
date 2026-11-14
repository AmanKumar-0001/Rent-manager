package com.tenantmanager.domain.repository

import com.tenantmanager.domain.model.Tenant
import kotlinx.coroutines.flow.Flow

interface TenantRepository {
    fun getAllTenants(): Flow<List<Tenant>>
    fun getActiveTenants(): Flow<List<Tenant>>
    suspend fun getTenantById(id: String): Tenant?
    suspend fun insertTenant(tenant: Tenant)
    suspend fun updateTenant(tenant: Tenant)
    suspend fun archiveTenant(id: String, leaveDate: String)
    suspend fun deleteTenant(id: String)
    suspend fun wipeAllData()
}

