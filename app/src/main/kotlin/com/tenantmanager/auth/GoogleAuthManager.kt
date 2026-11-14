package com.tenantmanager.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine

data class GoogleSignInAccount(
    val email: String,
    val displayName: String?,
    val idToken: String?
)

open class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val driveFileScope = Scope(SCOPE_DRIVE_FILE)

    open suspend fun getAccessToken(): Result<String> {
        return try {
            val authorizationClient = Identity.getAuthorizationClient(context)
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(driveFileScope))
                .build()

            val authResult = authorizationClient.authorize(request).await()
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(SecurityException("Access token not available. User authorization required."))
            }
        } catch (e: ApiException) {
            val msg = parseApiException(e)
            Result.failure(SecurityException(msg, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun requestAuthorization(activity: Activity): Result<AuthorizationResult> {
        return try {
            val authorizationClient = Identity.getAuthorizationClient(activity)
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(driveFileScope))
                .build()

            val authResult = authorizationClient.authorize(request).await()
            Result.success(authResult)
        } catch (e: ApiException) {
            val msg = parseApiException(e)
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    private fun parseApiException(e: ApiException): String {
        return when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR -> { // Code 10
                "Developer error (code 10): SHA-1 fingerprint or package name (com.tenantmanager) is not configured in Google Cloud Console OAuth 2.0 Client IDs."
            }
            12500 -> { // SIGN_IN_FAILED
                "Sign-in failed (code 12500): Check Google Play Services, OAuth consent screen, and ensure an OAuth client ID is created."
            }
            CommonStatusCodes.NETWORK_ERROR -> {
                "Network error: Unable to reach Google servers. Please check your internet connection."
            }
            CommonStatusCodes.CANCELED -> {
                "Sign-in was canceled by the user."
            }
            else -> {
                "Google authentication error (code ${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    companion object {
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resumeWith(kotlin.Result.success(result)) }
        addOnFailureListener { exception -> cont.resumeWith(kotlin.Result.failure(exception)) }
        addOnCanceledListener { cont.cancel() }
    }

