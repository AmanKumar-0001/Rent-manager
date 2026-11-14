package com.tenantmanager.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tenantmanager.data.local.dao.SyncLogDao
import com.tenantmanager.data.local.entity.SyncLogEntity
import com.tenantmanager.domain.repository.SheetsSyncSettings
import com.tenantmanager.domain.repository.SyncRepository
import com.tenantmanager.domain.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SyncRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val syncLogDao: SyncLogDao
) : SyncRepository {

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("sync_is_signed_in")
        val USER_EMAIL = stringPreferencesKey("sync_user_email")
        val INCLUDE_NOTES = booleanPreferencesKey("sync_include_notes")
        val AUTO_SYNC = booleanPreferencesKey("sync_auto_sync")
        val SPREADSHEET_ID = stringPreferencesKey("sync_spreadsheet_id")
        val SPREADSHEET_NAME = stringPreferencesKey("sync_spreadsheet_name")
        val LAST_SYNC_TIME = longPreferencesKey("sync_last_time")
        val LAST_SYNC_STATUS = stringPreferencesKey("sync_last_status")
    }

    override fun getSyncSettings(): Flow<SheetsSyncSettings> {
        return dataStore.data.map { prefs ->
            SheetsSyncSettings(
                isGoogleSignedIn = prefs[Keys.IS_SIGNED_IN] ?: false,
                userEmail = prefs[Keys.USER_EMAIL],
                includeNotesInSheet = prefs[Keys.INCLUDE_NOTES] ?: false,
                autoSyncEnabled = prefs[Keys.AUTO_SYNC] ?: false,
                spreadsheetId = prefs[Keys.SPREADSHEET_ID],
                spreadsheetName = prefs[Keys.SPREADSHEET_NAME] ?: "Tenant Manager",
                lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIME] ?: 0L,
                lastSyncStatus = prefs[Keys.LAST_SYNC_STATUS]
            )
        }
    }

    override suspend fun updateGoogleAccount(isSignedIn: Boolean, email: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = isSignedIn
            if (email != null) {
                prefs[Keys.USER_EMAIL] = email
            } else {
                prefs.remove(Keys.USER_EMAIL)
            }
        }
    }

    override suspend fun updateIncludeNotesInSheet(include: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.INCLUDE_NOTES] = include
        }
    }

    override suspend fun updateAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC] = enabled
        }
    }

    override suspend fun updateSpreadsheetInfo(id: String, name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SPREADSHEET_ID] = id
            prefs[Keys.SPREADSHEET_NAME] = name
        }
    }

    override suspend fun recordSyncResult(result: SyncResult) {
        when (result) {
            is SyncResult.Success -> {
                dataStore.edit { prefs ->
                    prefs[Keys.LAST_SYNC_TIME] = result.timestamp
                    prefs[Keys.LAST_SYNC_STATUS] = "Success (${result.itemsCount} items)"
                    prefs[Keys.SPREADSHEET_ID] = result.spreadsheetId
                }
                syncLogDao.insertLog(
                    SyncLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = result.timestamp,
                        status = "SUCCESS",
                        itemsSynced = result.itemsCount,
                        message = "Snapshot updated successfully"
                    )
                )
            }
            is SyncResult.Failure -> {
                dataStore.edit { prefs ->
                    prefs[Keys.LAST_SYNC_TIME] = result.timestamp
                    prefs[Keys.LAST_SYNC_STATUS] = "Failed: ${result.error}"
                    if (result.isAuthRevoked) {
                        prefs[Keys.IS_SIGNED_IN] = false
                    }
                }
                syncLogDao.insertLog(
                    SyncLogEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = result.timestamp,
                        status = "FAILURE",
                        itemsSynced = 0,
                        message = result.error
                    )
                )
            }
        }
    }

    fun getRecentSyncLogs(limit: Int = 10): Flow<List<SyncLogEntity>> {
        return syncLogDao.getRecentLogs(limit)
    }
}

