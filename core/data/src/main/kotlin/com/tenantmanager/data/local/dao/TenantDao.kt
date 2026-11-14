package com.tenantmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tenantmanager.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants ORDER BY isArchived ASC, name ASC")
    fun getAllTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: String): TenantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tenants: List<TenantEntity>)

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Query("UPDATE tenants SET isArchived = 1, leaveDate = :leaveDate, lastModified = :now WHERE id = :id")
    suspend fun archiveTenant(id: String, leaveDate: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM tenants WHERE id = :id")
    suspend fun deleteTenant(id: String)

    @Query("DELETE FROM tenants")
    suspend fun clearAll()
}

