package com.tenantmanager.domain.sync

import com.tenantmanager.domain.model.Payment
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant

data class SyncSnapshot(
    val properties: List<Property>,
    val tenants: List<Tenant>,
    val payments: List<Payment>,
    val includeNotes: Boolean = false,
    val currencySymbol: String = "₹",
    val syncTimestamp: Long = System.currentTimeMillis()
) {
    val totalItemsCount: Int
        get() = properties.size + tenants.size + payments.size
}

sealed class SyncResult {
    data class Success(
        val spreadsheetId: String,
        val spreadsheetUrl: String,
        val itemsCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult()

    data class Failure(
        val error: String,
        val isAuthRevoked: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult()
}

data class SheetTabData(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
) {
    val allValues: List<List<String>>
        get() = listOf(headers) + rows
}

