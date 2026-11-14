package com.tenantmanager.domain.sync

interface RemoteMirror {
    suspend fun sync(snapshot: SyncSnapshot, accessToken: String): SyncResult
}

class NoOpRemoteMirror : RemoteMirror {
    override suspend fun sync(snapshot: SyncSnapshot, accessToken: String): SyncResult {
        return SyncResult.Failure("Google Sheets sync is disabled in this configuration.")
    }
}

