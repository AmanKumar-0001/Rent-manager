package com.tenantmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tenantmanager.data.local.dao.AlertHistoryDao
import com.tenantmanager.data.local.dao.PaymentDao
import com.tenantmanager.data.local.dao.PropertyDao
import com.tenantmanager.data.local.dao.TenantDao
import com.tenantmanager.data.local.entity.AlertHistoryEntity
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity

@Database(
    entities = [
        PropertyEntity::class,
        TenantEntity::class,
        PaymentEntity::class,
        AlertHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun tenantDao(): TenantDao
    abstract fun paymentDao(): PaymentDao
    abstract fun alertHistoryDao(): AlertHistoryDao
}

