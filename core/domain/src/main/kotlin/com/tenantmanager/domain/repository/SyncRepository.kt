package com.tenantmanager.domain.repository

import com.tenantmanager.domain.sync.SyncResult
import kotlinx.coroutines.flow.Flow

data class SheetsSyncSettings(
    val isGoogleSignedIn: Boolean = false,
    val userEmail: String? = null,
    val includeNotesInSheet: Boolean = false,
    val autoSyncEnabled: Boolean = false,
    val spreadsheetId: String? = null,
    val spreadsheetName: String = "Collect Your Rent - Data",
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStatus: String? = null
) {
    val spreadsheetUrl: String?
        get() = spreadsheetId?.let { "https://docs.google.com/spreadsheets/d/$it" }
}

interface SyncRepository {
    fun getSyncSettings(): Flow<SheetsSyncSettings>
    suspend fun updateGoogleAccount(isSignedIn: Boolean, email: String?)
    suspend fun updateIncludeNotesInSheet(include: Boolean)
    suspend fun updateAutoSyncEnabled(enabled: Boolean)
    suspend fun updateSpreadsheetInfo(id: String, name: String)
    suspend fun recordSyncResult(result: SyncResult)
}

