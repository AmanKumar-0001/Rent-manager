package com.tenantmanager.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleAuthManagerTest {

    private lateinit var context: Context
    private lateinit var fakeClient: FakeAuthorizationClient
    private lateinit var authManager: GoogleAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeClient = FakeAuthorizationClient()
        authManager = GoogleAuthManager(context = context, client = fakeClient)
    }

    @Test
    fun directSuccess_returnsAccessTokenAndNoResolution() = runBlocking {
        val successResult = mockk<AuthorizationResult>()
        every { successResult.hasResolution() } returns false
        every { successResult.pendingIntent } returns null
        every { successResult.accessToken } returns "test_access_token_123"
        every { successResult.toGoogleSignInAccount() } returns null

        fakeClient.authorizeTask = Tasks.forResult(successResult)

        val initResult = authManager.requestAuthorization().getOrThrow()
        assertThat(initResult.hasResolution()).isFalse()
        assertThat(initResult.accessToken).isEqualTo("test_access_token_123")

        val tokenResult = authManager.getAccessToken().getOrThrow()
        assertThat(tokenResult).isEqualTo("test_access_token_123")
    }

    @Test
    fun resolutionRequired_resolutionSuccess_returnsToken() = runBlocking {
        val pendingIntent = mockk<PendingIntent>()
        val resolutionNeededResult = mockk<AuthorizationResult>()
        every { resolutionNeededResult.hasResolution() } returns true
        every { resolutionNeededResult.pendingIntent } returns pendingIntent
        every { resolutionNeededResult.accessToken } returns null

        fakeClient.authorizeTask = Tasks.forResult(resolutionNeededResult)

        val initResult = authManager.requestAuthorization().getOrThrow()
        assertThat(initResult.hasResolution()).isTrue()
        assertThat(initResult.pendingIntent).isEqualTo(pendingIntent)

        // Simulate activity launcher returning RESULT_OK with intent
        val mockIntent = Intent()
        val resolvedResult = mockk<AuthorizationResult>()
        every { resolvedResult.hasResolution() } returns false
        every { resolvedResult.accessToken } returns "resolved_token_456"
        every { resolvedResult.toGoogleSignInAccount() } returns null
        fakeClient.intentResult = resolvedResult

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_OK, mockIntent).getOrThrow()
        assertThat(handleResult.accessToken).isEqualTo("resolved_token_456")
    }

    @Test
    fun resolutionCancelled_returnsCancellationFailure() = runBlocking {
        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_CANCELED, null)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).contains("cancelled")
    }

    @Test
    fun authorizeFailure_developerError_returnsFormattedMessage() = runBlocking {
        fakeClient.authorizeTask = Tasks.forException(
            ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR, "Developer error"))
        )

        val initResult = authManager.requestAuthorization()
        assertThat(initResult.isFailure).isTrue()
        val exception = initResult.exceptionOrNull()
        assertThat(exception?.message).contains("com.tenantmanager")

        val tokenResult = authManager.getAccessToken()
        assertThat(tokenResult.isFailure).isTrue()
        assertThat(tokenResult.exceptionOrNull()?.message).contains("com.tenantmanager")
    }

    @Test
    fun authorizeFailure_playServicesError_returnsPlainLanguageMessage() = runBlocking {
        fakeClient.authorizeTask = Tasks.forException(
            ApiException(Status(12500, "Sign in failed"))
        )

        val initResult = authManager.requestAuthorization()
        assertThat(initResult.isFailure).isTrue()
        assertThat(initResult.exceptionOrNull()?.message).contains("Google Play Services")
    }

    @Test
    fun resolutionIntentFailure_returnsFailure() = runBlocking {
        val mockIntent = Intent()
        fakeClient.intentException = ApiException(Status(CommonStatusCodes.NETWORK_ERROR, "Network error"))

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_OK, mockIntent)
        assertThat(handleResult.isFailure).isTrue()
        assertThat(handleResult.exceptionOrNull()?.message).contains("Network error")
    }

    @Test
    fun serverClientId_configured_includedInRequest() = runBlocking {
        val customAuthManager = GoogleAuthManager(
            context = context,
            client = fakeClient,
            serverClientId = "test-web-client-id.apps.googleusercontent.com"
        )
        val successResult = mockk<AuthorizationResult>()
        every { successResult.hasResolution() } returns false
        every { successResult.accessToken } returns "web_token"
        every { successResult.toGoogleSignInAccount() } returns null
        fakeClient.authorizeTask = Tasks.forResult(successResult)

        customAuthManager.requestAuthorization()

        assertThat(fakeClient.lastRequest?.serverClientId).isEqualTo("test-web-client-id.apps.googleusercontent.com")
    }
}
