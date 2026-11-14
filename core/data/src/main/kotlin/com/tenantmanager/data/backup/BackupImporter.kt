package com.tenantmanager.data.backup

import androidx.room.withTransaction
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity
import kotlinx.serialization.json.Json

class BackupImporter(
    private val database: AppDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun importPlaintextJson(jsonContent: String) {
        val payload = json.decodeFromString<BackupPayload>(jsonContent)

        if (payload.schemaVersion > 1) {
            throw IllegalArgumentException("Unsupported backup schema version: ${payload.schemaVersion}. This app supports version 1.")
        }

        // Validate foreign keys: all payment tenantIds must exist in tenants
        val tenantIds = payload.tenants.map { it.id }.toSet()
        val invalidPayment = payload.payments.firstOrNull { it.tenantId !in tenantIds }
        if (invalidPayment != null) {
            throw IllegalArgumentException("Corrupted backup: payment ${invalidPayment.id} references non-existent tenant ${invalidPayment.tenantId}")
        }

        val propertyEntities = payload.properties.map {
            PropertyEntity(
                id = it.id,
                name = it.name,
                address = it.address,
                defaultRentMinor = it.defaultRentMinor,
                createdAt = it.createdAt,
                lastModified = it.lastModified
            )
        }

        val tenantEntities = payload.tenants.map {
            TenantEntity(
                id = it.id,
                propertyId = it.propertyId,
                name = it.name,
                phone = it.phone,
                email = it.email,
                unitName = it.unitName,
                joinDate = it.joinDate,
                leaveDate = it.leaveDate,
                monthlyRentMinor = it.monthlyRentMinor,
                dueDay = it.dueDay,
                securityDepositMinor = it.securityDepositMinor,
                depositReturnedDate = it.depositReturnedDate,
                notes = it.notes,
                isArchived = it.isArchived,
                createdAt = it.createdAt,
                lastModified = it.lastModified
            )
        }

        val paymentEntities = payload.payments.map {
            PaymentEntity(
                id = it.id,
                tenantId = it.tenantId,
                amountMinor = it.amountMinor,
                paidDate = it.paidDate,
                periodMonth = it.periodMonth,
                notes = it.notes,
                createdAt = it.createdAt,
                lastModified = it.lastModified
            )
        }

        database.withTransaction {
            database.paymentDao().clearAll()
            database.tenantDao().clearAll()
            database.propertyDao().clearAll()

            database.propertyDao().insertAll(propertyEntities)
            database.tenantDao().insertAll(tenantEntities)
            database.paymentDao().insertAll(paymentEntities)
        }
    }

    suspend fun importEncrypted(encryptedBytes: ByteArray, passphrase: CharArray) {
        val decryptedBytes = CryptoEngine.decrypt(encryptedBytes, passphrase)
        val jsonContent = String(decryptedBytes, Charsets.UTF_8)
        importPlaintextJson(jsonContent)
    }
}

