package com.tenantmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tenantmanager.data.local.dao.AlertHistoryDao
import com.tenantmanager.data.local.dao.PaymentDao
import com.tenantmanager.data.local.dao.PropertyDao
import com.tenantmanager.data.local.dao.SyncLogDao
import com.tenantmanager.data.local.dao.TenantDao
import com.tenantmanager.data.local.entity.AlertHistoryEntity
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.SyncLogEntity
import com.tenantmanager.data.local.entity.TenantEntity

@Database(
    entities = [
        PropertyEntity::class,
        TenantEntity::class,
        PaymentEntity::class,
        AlertHistoryEntity::class,
        SyncLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun tenantDao(): TenantDao
    abstract fun paymentDao(): PaymentDao
    abstract fun alertHistoryDao(): AlertHistoryDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_logs` (
                        `id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `itemsSynced` INTEGER NOT NULL,
                        `message` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

