package com.tenantmanager.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tenantmanager.BuildConfig
import com.tenantmanager.TenantApplication
import com.tenantmanager.di.AppContainer
import com.tenantmanager.di.DefaultAppContainer
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import kotlinx.coroutines.flow.first

class SheetsSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val customContainer: AppContainer? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!BuildConfig.SYNC_ENABLED) {
            return Result.success()
        }

        val container = customContainer
            ?: (applicationContext as? TenantApplication)?.container
            ?: DefaultAppContainer(applicationContext)

        val syncSettings = container.syncRepository.getSyncSettings().first()
        if (!syncSettings.isGoogleSignedIn || !syncSettings.autoSyncEnabled) {
            return Result.success()
        }

        val tokenResult = container.googleAuthManager.getAccessToken()
        if (tokenResult.isFailure) {
            val exception = tokenResult.exceptionOrNull()
            if (exception is SecurityException) {
                container.syncRepository.recordSyncResult(
                    SyncResult.Failure(exception.message ?: "Authentication required", isAuthRevoked = true)
                )
                return Result.failure()
            }
            return Result.retry()
        }

        val accessToken = tokenResult.getOrThrow()
        val properties = container.propertyRepository.getAllProperties().first()
        val tenants = container.tenantRepository.getAllTenants().first()
        val payments = container.paymentRepository.getAllPayments().first()
        val appSettings = container.settingsRepository.getSettings().first()

        val snapshot = SyncSnapshot(
            properties = properties,
            tenants = tenants,
            payments = payments,
            includeNotes = syncSettings.includeNotesInSheet,
            currencySymbol = appSettings.currencySymbol
        )

        val syncResult = container.remoteMirror.sync(snapshot, accessToken)
        container.syncRepository.recordSyncResult(syncResult)

        return when (syncResult) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Failure -> {
                if (syncResult.isAuthRevoked) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }
}

