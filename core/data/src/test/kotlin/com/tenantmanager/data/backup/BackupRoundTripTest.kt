package com.tenantmanager.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tenantmanager.data.local.AppDatabase
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context
    private lateinit var exporter: BackupExporter
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exporter = BackupExporter(database)
        importer = BackupImporter(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun populateData() {
        val property = PropertyEntity(
            id = "p1",
            name = "Sunrise Heights",
            address = "742 Evergreen Terr",
            defaultRentMinor = 2500000L
        )
        database.propertyDao().insertProperty(property)

        val tenant = TenantEntity(
            id = "t1",
            propertyId = "p1",
            name = "Rohan Verma",
            phone = "9876543210",
            email = "rohan@example.com",
            unitName = "4B",
            joinDate = "2026-01-01",
            leaveDate = null,
            monthlyRentMinor = 2500000L,
            dueDay = 5,
            securityDepositMinor = 5000000L,
            notes = "Prefers UPI"
        )
        database.tenantDao().insertTenant(tenant)

        val payment = PaymentEntity(
            id = "pay1",
            tenantId = "t1",
            amountMinor = 2500000L,
            paidDate = "2026-01-05",
            periodMonth = "2026-01",
            notes = "January Rent Paid"
        )
        database.paymentDao().insertPayment(payment)
    }

    @Test
    fun testPlaintextBackupRoundTrip() = runBlocking {
        populateData()

        val json = exporter.exportPlaintextJson()
        assertThat(json).contains("\"schemaVersion\": 1")
        assertThat(json).contains("Rohan Verma")
        assertThat(json).contains("Sunrise Heights")

        // Wipe database
        database.paymentDao().clearAll()
        database.tenantDao().clearAll()
        database.propertyDao().clearAll()

        assertThat(database.tenantDao().getAllTenants().first()).isEmpty()
        assertThat(database.propertyDao().getAllProperties().first()).isEmpty()
        assertThat(database.paymentDao().getAllPayments().first()).isEmpty()

        // Restore
        importer.importPlaintextJson(json)

        val restoredProperties = database.propertyDao().getAllProperties().first()
        val restoredTenants = database.tenantDao().getAllTenants().first()
        val restoredPayments = database.paymentDao().getAllPayments().first()

        assertThat(restoredProperties).hasSize(1)
        assertThat(restoredProperties.first().name).isEqualTo("Sunrise Heights")

        assertThat(restoredTenants).hasSize(1)
        assertThat(restoredTenants.first().name).isEqualTo("Rohan Verma")

        assertThat(restoredPayments).hasSize(1)
        assertThat(restoredPayments.first().notes).isEqualTo("January Rent Paid")
    }

    @Test
    fun testEncryptedBackupRoundTrip() = runBlocking {
        populateData()
        val passphrase = "SuperSecurePassword123!".toCharArray()

        val encryptedBytes = exporter.exportEncrypted(passphrase)
        assertThat(encryptedBytes.size).isGreaterThan(60)

        // Clear database
        database.paymentDao().clearAll()
        database.tenantDao().clearAll()
        database.propertyDao().clearAll()

        // Restore with correct password
        importer.importEncrypted(encryptedBytes, passphrase)

        val restoredTenants = database.tenantDao().getAllTenants().first()
        assertThat(restoredTenants).hasSize(1)
        assertThat(restoredTenants.first().name).isEqualTo("Rohan Verma")
    }

    @Test
    fun testWrongPassphraseThrowsGenericSecurityException() = runBlocking {
        populateData()
        val correctPassphrase = "SuperSecurePassword123!".toCharArray()
        val wrongPassphrase = "WrongPassword".toCharArray()

        val encryptedBytes = exporter.exportEncrypted(correctPassphrase)

        val exception = assertThrows(SecurityException::class.java) {
            runBlocking {
                importer.importEncrypted(encryptedBytes, wrongPassphrase)
            }
        }
        assertThat(exception.message).isEqualTo("Failed to decrypt: invalid passphrase or corrupted file")
    }

    @Test
    fun testTamperedFileThrowsGenericSecurityException() = runBlocking {
        populateData()
        val passphrase = "SuperSecurePassword123!".toCharArray()

        val encryptedBytes = exporter.exportEncrypted(passphrase)

        // Tamper with ciphertext by flipping bits
        val tamperedBytes = encryptedBytes.copyOf()
        tamperedBytes[tamperedBytes.size - 5] = (tamperedBytes[tamperedBytes.size - 5].toInt() xor 0xFF).toByte()

        val exception = assertThrows(SecurityException::class.java) {
            runBlocking {
                importer.importEncrypted(tamperedBytes, passphrase)
            }
        }
        assertThat(exception.message).isEqualTo("Failed to decrypt: invalid passphrase or corrupted file")
    }

    @Test
    fun testCorruptedHeaderThrowsGenericSecurityException() = runBlocking {
        val badHeaderBytes = ByteArray(100) { 0 }
        val exception = assertThrows(SecurityException::class.java) {
            runBlocking {
                importer.importEncrypted(badHeaderBytes, "pass".toCharArray())
            }
        }
        assertThat(exception.message).isEqualTo("Failed to decrypt: invalid passphrase or corrupted file")
    }

    @Test
    fun testUnsupportedSchemaVersionThrowsException() {
        val futureJson = """
            {
              "schemaVersion": 99,
              "exportedAt": "2026-11-14T12:00:00Z",
              "properties": [],
              "tenants": [],
              "payments": []
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                importer.importPlaintextJson(futureJson)
            }
        }
        assertThat(exception.message).contains("Unsupported backup schema version: 99")
    }

    @Test
    fun testInvalidForeignKeyThrowsException() {
        val corruptedJson = """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-11-14T12:00:00Z",
              "properties": [],
              "tenants": [],
              "payments": [
                {
                  "id": "p1",
                  "tenantId": "non-existent-tenant",
                  "amountMinor": 10000,
                  "paidDate": "2026-01-01",
                  "periodMonth": "2026-01"
                }
              ]
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                importer.importPlaintextJson(corruptedJson)
            }
        }
        assertThat(exception.message).contains("references non-existent tenant")
    }
}

