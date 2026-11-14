package com.tenantmanager.data.backup

import androidx.room.withTransaction
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class BackupExporter(
    private val database: AppDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportPlaintextJson(): String {
        val properties = database.propertyDao().getAllProperties().first().map {
            PropertyBackupDto(
                id = it.id,
                name = it.name,
                address = it.address,
                defaultRentMinor = it.defaultRentMinor,
                createdAt = it.createdAt,
                lastModified = it.lastModified
            )
        }

        val tenants = database.tenantDao().getAllTenants().first().map {
            TenantBackupDto(
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

        val payments = database.paymentDao().getAllPayments().first().map {
            PaymentBackupDto(
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

        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = Instant.now().toString(),
            properties = properties,
            tenants = tenants,
            payments = payments
        )

        return json.encodeToString(payload)
    }

    suspend fun exportEncrypted(passphrase: CharArray): ByteArray {
        val plaintextJson = exportPlaintextJson()
        return CryptoEngine.encrypt(plaintextJson.toByteArray(Charsets.UTF_8), passphrase)
    }
}

