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
    fun resolutionRequired_evenIfResultCanceled_withValidIntentData_returnsToken() = runBlocking {
        // Crucial test: When resultCode is RESULT_CANCELED (0) but intent data contains valid result,
        // we must not decide success or cancellation from resultCode alone!
        val mockIntent = Intent()
        val resolvedResult = mockk<AuthorizationResult>()
        every { resolvedResult.hasResolution() } returns false
        every { resolvedResult.accessToken } returns "resolved_token_456"
        every { resolvedResult.toGoogleSignInAccount() } returns null
        fakeClient.intentResult = resolvedResult

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_CANCELED, mockIntent).getOrThrow()
        assertThat(handleResult.accessToken).isEqualTo("resolved_token_456")
    }

    @Test
    fun userCancelled_nullDataAndResultCanceled_returnsCancellationMessage() = runBlocking {
        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_CANCELED, null)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).isEqualTo("Google sign-in was cancelled. Tap Sign in with Google to try again.")
    }

    @Test
    fun userCancelled_statusCode16_returnsCancellationMessage() = runBlocking {
        val mockIntent = Intent()
        fakeClient.intentException = ApiException(Status(CommonStatusCodes.CANCELED, "User cancelled"))

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_OK, mockIntent)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).isEqualTo("Google sign-in was cancelled. Tap Sign in with Google to try again.")
    }

    @Test
    fun otherFailureCode_code10_returnsTestUserMessage() = runBlocking {
        val mockIntent = Intent()
        fakeClient.intentException = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR, "Developer error"))

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_OK, mockIntent)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).isEqualTo("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again.")
    }

    @Test
    fun otherFailureCode_code8_internalError_returnsTestUserMessage() = runBlocking {
        val mockIntent = Intent()
        fakeClient.intentException = ApiException(Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error"))

        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_OK, mockIntent)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).isEqualTo("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again.")
    }

    @Test
    fun otherFailureCode_nullData_resultNotCanceled_returnsTestUserMessage() = runBlocking {
        val handleResult = authManager.handleAuthorizationResult(Activity.RESULT_FIRST_USER, null)
        assertThat(handleResult.isFailure).isTrue()
        val exception = handleResult.exceptionOrNull()
        assertThat(exception).isInstanceOf(SecurityException::class.java)
        assertThat(exception?.message).isEqualTo("Google sign-in did not finish. Check that Google Play Services is up to date and that this Google account is a test user in the Google Cloud project, then try again.")
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
