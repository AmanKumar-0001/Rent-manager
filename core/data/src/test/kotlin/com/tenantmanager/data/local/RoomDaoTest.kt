package com.tenantmanager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tenantmanager.data.local.entity.AlertHistoryEntity
import com.tenantmanager.data.local.entity.PaymentEntity
import com.tenantmanager.data.local.entity.PropertyEntity
import com.tenantmanager.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPropertyAndTenantCRUD() = runBlocking {
        val property = PropertyEntity(
            id = "p1",
            name = "Oak Residency",
            address = "12 Main St",
            defaultRentMinor = 1500000L
        )
        database.propertyDao().insertProperty(property)

        val tenant = TenantEntity(
            id = "t1",
            propertyId = "p1",
            name = "Alice",
            phone = "9988776655",
            unitName = "101",
            joinDate = "2026-01-01",
            monthlyRentMinor = 1500000L,
            dueDay = 5
        )
        database.tenantDao().insertTenant(tenant)

        val fetchedTenant = database.tenantDao().getTenantById("t1")
        assertThat(fetchedTenant).isNotNull()
        assertThat(fetchedTenant?.name).isEqualTo("Alice")
        assertThat(fetchedTenant?.propertyId).isEqualTo("p1")

        // Archive tenant
        database.tenantDao().archiveTenant("t1", "2026-12-31")
        val archived = database.tenantDao().getTenantById("t1")
        assertThat(archived?.isArchived).isTrue()
        assertThat(archived?.leaveDate).isEqualTo("2026-12-31")

        val activeList = database.tenantDao().getActiveTenants().first()
        assertThat(activeList).isEmpty()

        val allList = database.tenantDao().getAllTenants().first()
        assertThat(allList).hasSize(1)
    }

    @Test
    fun testPaymentCascadeDeleteOnTenantDelete() = runBlocking {
        val tenant = TenantEntity(
            id = "t1",
            name = "Bob",
            phone = "1122334455",
            unitName = "202",
            joinDate = "2026-01-01",
            monthlyRentMinor = 1000000L,
            dueDay = 1
        )
        database.tenantDao().insertTenant(tenant)

        val payment = PaymentEntity(
            id = "pay1",
            tenantId = "t1",
            amountMinor = 1000000L,
            paidDate = "2026-01-01",
            periodMonth = "2026-01"
        )
        database.paymentDao().insertPayment(payment)

        val paymentsBefore = database.paymentDao().getPaymentsForTenant("t1").first()
        assertThat(paymentsBefore).hasSize(1)

        // Hard delete tenant
        database.tenantDao().deleteTenant("t1")

        // Payment should be cascade deleted!
        val paymentsAfter = database.paymentDao().getPaymentsForTenant("t1").first()
        assertThat(paymentsAfter).isEmpty()
    }

    @Test
    fun testPropertyDeleteSetsTenantPropertyIdToNull() = runBlocking {
        val property = PropertyEntity(id = "p1", name = "Cedar Woods")
        database.propertyDao().insertProperty(property)

        val tenant = TenantEntity(
            id = "t1",
            propertyId = "p1",
            name = "Charlie",
            phone = "3344556677",
            unitName = "303",
            joinDate = "2026-02-01",
            monthlyRentMinor = 1200000L,
            dueDay = 5
        )
        database.tenantDao().insertTenant(tenant)

        database.propertyDao().deleteProperty("p1")

        val tenantAfter = database.tenantDao().getTenantById("t1")
        assertThat(tenantAfter).isNotNull()
        assertThat(tenantAfter?.propertyId).isNull()
    }

    @Test
    fun testAlertHistoryDeduplication() = runBlocking {
        val tenant = TenantEntity(
            id = "t1",
            name = "David",
            phone = "4455667788",
            unitName = "404",
            joinDate = "2026-01-01",
            monthlyRentMinor = 800000L,
            dueDay = 5
        )
        database.tenantDao().insertTenant(tenant)

        val alert = AlertHistoryEntity(
            id = "a1",
            tenantId = "t1",
            periodMonth = "2026-11",
            alertType = "DUE_SOON"
        )

        assertThat(database.alertHistoryDao().hasAlertBeenSent("t1", "2026-11", "DUE_SOON")).isFalse()

        database.alertHistoryDao().recordAlert(alert)
        assertThat(database.alertHistoryDao().hasAlertBeenSent("t1", "2026-11", "DUE_SOON")).isTrue()

        // Insert duplicate alert with same key should be ignored
        val duplicateAlert = AlertHistoryEntity(
            id = "a2",
            tenantId = "t1",
            periodMonth = "2026-11",
            alertType = "DUE_SOON"
        )
        database.alertHistoryDao().recordAlert(duplicateAlert)
        assertThat(database.alertHistoryDao().hasAlertBeenSent("t1", "2026-11", "DUE_SOON")).isTrue()
    }
}

