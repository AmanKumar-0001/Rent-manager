package com.tenantmanager.data.sync

import com.tenantmanager.domain.sync.PrivacyFilter
import com.tenantmanager.domain.sync.RemoteMirror
import com.tenantmanager.domain.sync.SheetRowMapper
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SheetsRemoteMirror(
    private val apiClient: SheetsApiClient
) : RemoteMirror {

    override suspend fun sync(snapshot: SyncSnapshot, accessToken: String): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Sanitize snapshot via PrivacyFilter
                val sanitizedSnapshot = PrivacyFilter.filterSnapshot(snapshot)

                // 2. Map to the 3 tabs
                val tabs = SheetRowMapper.mapToTabs(sanitizedSnapshot)

                // 3. Find or create spreadsheet via drive.file scope
                val spreadsheetRef = apiClient.findOrCreateSpreadsheet(
                    title = "Tenant Manager",
                    tabTitles = tabs.map { it.title },
                    accessToken = accessToken
                )

                // 4. Batch update values in ONE request
                val success = apiClient.batchUpdateValues(
                    spreadsheetId = spreadsheetRef.id,
                    tabs = tabs,
                    accessToken = accessToken
                )

                if (success) {
                    SyncResult.Success(
                        spreadsheetId = spreadsheetRef.id,
                        spreadsheetUrl = spreadsheetRef.url,
                        itemsCount = sanitizedSnapshot.totalItemsCount
                    )
                } else {
                    SyncResult.Failure("Failed to update Google Sheet tabs.")
                }
            } catch (e: SecurityException) {
                SyncResult.Failure(
                    error = e.message ?: "Google authorization revoked or expired.",
                    isAuthRevoked = true
                )
            } catch (e: Exception) {
                SyncResult.Failure(
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }
}

