package com.tenantmanager.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class GoogleAuthManager(
    private val context: Context,
    private val client: AuthorizationClient? = null,
    private val serverClientId: String? = null
) {
    private val TAG = "GoogleAuthManager"
    private val credentialManager by lazy { CredentialManager.create(context) }
    private val driveFileScope = Scope(SCOPE_DRIVE_FILE)

    val authorizationClient: AuthorizationClient
        get() = client ?: Identity.getAuthorizationClient(context)

    fun buildAuthorizationRequest(): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveFileScope))
        if (!serverClientId.isNullOrBlank()) {
            builder.requestOfflineAccess(serverClientId)
        }
        return builder.build()
    }

    /**
     * Step 1 of Sign-In: Requests authorization for https://www.googleapis.com/auth/drive.file scope.
     * If the user has not yet consented, the returned AuthorizationResult has hasResolution() == true
     * and a non-null pendingIntent that must be launched via an ActivityResultLauncher.
     * If the user already authorized, hasResolution() == false and accessToken is available immediately.
     */
    open suspend fun requestAuthorization(activity: Activity? = null): Result<AuthorizationResult> {
        return try {
            val authClient = client ?: if (activity != null) {
                Identity.getAuthorizationClient(activity)
            } else {
                authorizationClient
            }
            val request = buildAuthorizationRequest()
            val authResult = authClient.authorize(request).await()
            Result.success(authResult)
        } catch (e: ApiException) {
            val msg = formatApiExceptionMessage(e)
            Log.w(TAG, "Authorization failed with ApiException statusCode=${e.statusCode}: ${e.status.statusMessage}")
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Log.w(TAG, "Authorization failed with exception: ${e::class.java.simpleName}")
            Result.failure(e)
        }
    }

    /**
     * Step 2 of Sign-In: Handles the result from the ActivityResultLauncher after launching the pendingIntent.
     * For an Android OAuth client, the access token is returned directly in the AuthorizationResult.
     */
    open fun handleAuthorizationResult(resultCode: Int, data: Intent?): Result<AuthorizationResult> {
        if (resultCode == Activity.RESULT_CANCELED) {
            Log.w(TAG, "Authorization resolution was cancelled by user (RESULT_CANCELED)")
            return Result.failure(SecurityException("Google sign-in was cancelled."))
        }
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Authorization resolution failed with resultCode=$resultCode")
            return Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and try again."))
        }
        if (data == null) {
            Log.w(TAG, "Authorization resolution succeeded but Intent data was null")
            return Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and try again."))
        }
        return try {
            val authResult = authorizationClient.getAuthorizationResultFromIntent(data)
            Result.success(authResult)
        } catch (e: ApiException) {
            val msg = formatApiExceptionMessage(e)
            Log.w(TAG, "getAuthorizationResultFromIntent failed with ApiException statusCode=${e.statusCode}: ${e.status.statusMessage}")
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Log.w(TAG, "getAuthorizationResultFromIntent failed with exception: ${e::class.java.simpleName}")
            Result.failure(e)
        }
    }

    /**
     * Requests a fresh access token before each sync.
     * Calls authorize() silently. If user consent was revoked or expired, hasResolution() will be true,
     * in which case this returns failure prompting the user to reconnect in Settings.
     * Access tokens are never stored in plain text or persisted on disk.
     */
    open suspend fun getAccessToken(): Result<String> {
        return try {
            val request = buildAuthorizationRequest()
            val authResult = authorizationClient.authorize(request).await()
            if (authResult.hasResolution()) {
                Log.w(TAG, "getAccessToken: authorization requires user resolution (consent expired or revoked)")
                Result.failure(SecurityException("Google authorization expired or revoked. Please reconnect in Settings."))
            } else {
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    Result.success(token)
                } else {
                    Log.w(TAG, "getAccessToken: access token was null/blank in result")
                    Result.failure(SecurityException("Google authorization expired or revoked. Please reconnect in Settings."))
                }
            }
        } catch (e: ApiException) {
            val msg = formatApiExceptionMessage(e)
            Log.w(TAG, "getAccessToken failed with ApiException statusCode=${e.statusCode}: ${e.status.statusMessage}")
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Log.w(TAG, "getAccessToken failed with exception: ${e::class.java.simpleName}")
            Result.failure(e)
        }
    }

    open suspend fun signOut(): Result<Unit> {
        return try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            try {
                Identity.getSignInClient(context).signOut().await()
            } catch (_: Exception) {
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "signOut failed: ${e::class.java.simpleName}")
            Result.failure(e)
        }
    }

    fun formatApiExceptionMessage(e: ApiException): String {
        return when (e.statusCode) {
            CommonStatusCodes.CANCELED -> { // 16
                "Google sign-in was cancelled."
            }
            CommonStatusCodes.DEVELOPER_ERROR -> { // 10
                "Google sign-in failed (code 10). The app's package name (com.tenantmanager) or SHA-1 fingerprint is not registered in Google Cloud Console."
            }
            12500 -> { // SIGN_IN_FAILED
                "Google sign-in did not finish. Check that Google Play Services is up to date and try again."
            }
            CommonStatusCodes.NETWORK_ERROR -> { // 7
                "Network error: Unable to reach Google servers. Please check your internet connection."
            }
            CommonStatusCodes.TIMEOUT -> { // 15
                "Connection timed out. Please check your internet connection and try again."
            }
            CommonStatusCodes.API_NOT_CONNECTED,
            CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED,
            CommonStatusCodes.SERVICE_DISABLED -> {
                "Google Play Services is unavailable or out of date. Please update Google Play Services and try again."
            }
            else -> {
                "Google sign-in did not finish. Check that Google Play Services is up to date and try again."
            }
        }
    }

    companion object {
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    }
}

internal suspend fun <T> Task<T>.await(): T {
    if (isComplete) {
        val e = exception
        return if (e != null) {
            throw e
        } else if (isCanceled) {
            throw kotlinx.coroutines.CancellationException("Task $this was cancelled.")
        } else {
            @Suppress("UNCHECKED_CAST")
            result as T
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception -> cont.resumeWithException(exception) }
        addOnCanceledListener { cont.cancel() }
    }
}
