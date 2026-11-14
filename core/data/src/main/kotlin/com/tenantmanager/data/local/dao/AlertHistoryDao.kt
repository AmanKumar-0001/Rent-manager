package com.tenantmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tenantmanager.data.local.entity.AlertHistoryEntity

@Dao
interface AlertHistoryDao {
    @Query("SELECT COUNT(*) > 0 FROM alert_history WHERE tenantId = :tenantId AND periodMonth = :periodMonth AND alertType = :alertType")
    suspend fun hasAlertBeenSent(tenantId: String, periodMonth: String, alertType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordAlert(alert: AlertHistoryEntity)

    @Query("DELETE FROM alert_history")
    suspend fun clearAll()
}

