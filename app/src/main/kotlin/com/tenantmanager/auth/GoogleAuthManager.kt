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
            if (!authResult.hasResolution() && !authResult.accessToken.isNullOrBlank()) {
                Log.d(TAG, "Authorization request: success (code ${CommonStatusCodes.SUCCESS})")
            }
            Result.success(authResult)
        } catch (e: ApiException) {
            val msg = formatApiExceptionMessage(e)
            Log.d(TAG, "Authorization request: ${if (e.statusCode == CommonStatusCodes.CANCELED) "cancelled" else "error"} (code ${e.statusCode})")
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Log.d(TAG, "Authorization request: error (code ${CommonStatusCodes.ERROR})")
            Result.failure(e)
        }
    }

    /**
     * Step 2 of Sign-In: Handles the result from the ActivityResultLauncher after launching the pendingIntent.
     * In the result callback, success or cancellation is NOT decided from resultCode alone.
     * Instead, Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data) is inspected.
     * For an Android OAuth client, the access token is returned directly in the AuthorizationResult.
     */
    open fun handleAuthorizationResult(resultCode: Int, data: Intent?): Result<AuthorizationResult> {
        if (data == null) {
            val statusCode = if (resultCode == Activity.RESULT_CANCELED) {
                CommonStatusCodes.CANCELED
            } else {
                CommonStatusCodes.INTERNAL_ERROR
            }
            return if (statusCode == CommonStatusCodes.CANCELED) {
                Log.d(TAG, "Authorization result: cancelled (code $statusCode)")
                Result.failure(SecurityException("Google sign-in was cancelled. Tap Sign in with Google to try again."))
            } else {
                Log.d(TAG, "Authorization result: error (code $statusCode)")
                Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."))
            }
        }

        return try {
            val authResult = authorizationClient.getAuthorizationResultFromIntent(data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                Log.d(TAG, "Authorization result: success (code ${CommonStatusCodes.SUCCESS})")
                Result.success(authResult)
            } else {
                Log.d(TAG, "Authorization result: error (code ${CommonStatusCodes.INTERNAL_ERROR})")
                Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."))
            }
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            if (statusCode == CommonStatusCodes.CANCELED) {
                Log.d(TAG, "Authorization result: cancelled (code $statusCode)")
                Result.failure(SecurityException("Google sign-in was cancelled. Tap Sign in with Google to try again.", e))
            } else {
                Log.d(TAG, "Authorization result: error (code $statusCode)")
                Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again.", e))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Authorization result: error (code ${CommonStatusCodes.ERROR})")
            Result.failure(SecurityException("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again.", e))
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
                Log.d(TAG, "Authorization getAccessToken: resolution required")
                Result.failure(SecurityException("Google authorization expired or revoked. Please reconnect in Settings."))
            } else {
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    Log.d(TAG, "Authorization getAccessToken: success (code ${CommonStatusCodes.SUCCESS})")
                    Result.success(token)
                } else {
                    Log.d(TAG, "Authorization getAccessToken: error (missing accessToken)")
                    Result.failure(SecurityException("Google authorization expired or revoked. Please reconnect in Settings."))
                }
            }
        } catch (e: ApiException) {
            val msg = formatApiExceptionMessage(e)
            Log.d(TAG, "Authorization getAccessToken: ${if (e.statusCode == CommonStatusCodes.CANCELED) "cancelled" else "error"} (code ${e.statusCode})")
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Log.d(TAG, "Authorization getAccessToken: error (code ${CommonStatusCodes.ERROR})")
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
                "Google sign-in was cancelled. Tap Sign in with Google to try again."
            }
            else -> {
                "Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again."
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
