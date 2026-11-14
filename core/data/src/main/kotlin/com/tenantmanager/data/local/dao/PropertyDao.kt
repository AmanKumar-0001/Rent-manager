package com.tenantmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tenantmanager.data.local.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY name ASC")
    fun getAllProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: String): PropertyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(properties: List<PropertyEntity>)

    @Update
    suspend fun updateProperty(property: PropertyEntity)

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deleteProperty(id: String)

    @Query("DELETE FROM properties")
    suspend fun clearAll()
}

