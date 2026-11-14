package com.tenantmanager.data.sync

import com.tenantmanager.domain.sync.PrivacyFilter
import com.tenantmanager.domain.sync.RemoteMirror
import com.tenantmanager.domain.sync.SheetRowMapper
import com.tenantmanager.domain.sync.SheetTabData
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import java.io.IOException

class FakeSheetsApiClient : SheetsApiClient {

    var shouldFailWithAuthRevoked: Boolean = false
    var shouldFailWithNetworkError: Boolean = false

    var batchUpdateCallsCount: Int = 0
    val uploadedTabs = mutableMapOf<String, SheetTabData>()
    var createdSpreadsheetTitle: String? = null
    var lastAccessTokenUsed: String? = null

    override suspend fun findOrCreateSpreadsheet(
        title: String,
        tabTitles: List<String>,
        accessToken: String
    ): SpreadsheetRef {
        if (shouldFailWithAuthRevoked) {
            throw SecurityException("Google authorization expired or revoked (HTTP 401)")
        }
        if (shouldFailWithNetworkError) {
            throw IOException("Network error: Unable to resolve host")
        }

        createdSpreadsheetTitle = title
        lastAccessTokenUsed = accessToken

        return SpreadsheetRef(
            id = "fake-sheet-id-12345",
            url = "https://docs.google.com/spreadsheets/d/fake-sheet-id-12345",
            isNewlyCreated = true
        )
    }

    override suspend fun batchUpdateValues(
        spreadsheetId: String,
        tabs: List<SheetTabData>,
        accessToken: String
    ): Boolean {
        if (shouldFailWithAuthRevoked) {
            throw SecurityException("Google authorization expired or revoked (HTTP 401)")
        }
        if (shouldFailWithNetworkError) {
            throw IOException("Network error during batch update")
        }

        batchUpdateCallsCount++
        lastAccessTokenUsed = accessToken
        uploadedTabs.clear()
        for (tab in tabs) {
            uploadedTabs[tab.title] = tab
        }

        return true
    }
}

class FakeRemoteMirror(
    val fakeApiClient: FakeSheetsApiClient = FakeSheetsApiClient()
) : RemoteMirror {

    private val delegate = SheetsRemoteMirror(fakeApiClient)

    override suspend fun sync(snapshot: SyncSnapshot, accessToken: String): SyncResult {
        return delegate.sync(snapshot, accessToken)
    }
}

