package com.tenantmanager.ui.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.tenantmanager.auth.FakeAuthorizationClient
import com.tenantmanager.auth.GoogleAuthManager
import com.tenantmanager.domain.model.Property
import com.tenantmanager.domain.model.Tenant
import com.tenantmanager.domain.repository.AppSettings
import com.tenantmanager.domain.repository.PaymentRepository
import com.tenantmanager.domain.repository.PropertyRepository
import com.tenantmanager.domain.repository.SettingsRepository
import com.tenantmanager.domain.repository.SheetsSyncSettings
import com.tenantmanager.domain.repository.SyncRepository
import com.tenantmanager.domain.repository.TenantRepository
import com.tenantmanager.domain.sync.RemoteMirror
import com.tenantmanager.domain.sync.SyncResult
import com.tenantmanager.domain.sync.SyncSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    private val settingsFlow = MutableStateFlow(AppSettings())
    private val syncSettingsFlow = MutableStateFlow(SheetsSyncSettings())

    private lateinit var fakeClient: FakeAuthorizationClient
    private lateinit var authManager: GoogleAuthManager
    private lateinit var syncRepository: SyncRepository
    private lateinit var fakeMirror: FakeTestMirror

    private lateinit var viewModel: SettingsViewModel

    class FakeTestMirror : RemoteMirror {
        var lastSnapshot: SyncSnapshot? = null
        var lastToken: String? = null
        var resultToReturn: SyncResult = SyncResult.Success("test_sheet_id", "https://sheets.google.com/test", 3)

        override suspend fun sync(snapshot: SyncSnapshot, accessToken: String): SyncResult {
            lastSnapshot = snapshot
            lastToken = accessToken
            return resultToReturn
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        fakeClient = FakeAuthorizationClient()
        authManager = GoogleAuthManager(context = context, client = fakeClient)
        fakeMirror = FakeTestMirror()

        val mockSettingsRepo = mockk<SettingsRepository>(relaxed = true) {
            every { getSettings() } returns settingsFlow.asStateFlow()
        }
        val mockTenantRepo = mockk<TenantRepository>(relaxed = true) {
            every { getAllTenants() } returns MutableStateFlow<List<Tenant>>(emptyList())
        }
        val mockPropertyRepo = mockk<PropertyRepository>(relaxed = true) {
            every { getAllProperties() } returns MutableStateFlow<List<Property>>(emptyList())
        }
        val mockPaymentRepo = mockk<PaymentRepository>(relaxed = true) {
            every { getAllPayments() } returns MutableStateFlow(emptyList())
        }
        syncRepository = object : SyncRepository {
            override fun getSyncSettings(): Flow<SheetsSyncSettings> = syncSettingsFlow.asStateFlow()
            override suspend fun updateGoogleAccount(isSignedIn: Boolean, email: String?) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(
                    isGoogleSignedIn = isSignedIn,
                    userEmail = email
                )
            }
            override suspend fun updateIncludeNotesInSheet(include: Boolean) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(includeNotesInSheet = include)
            }
            override suspend fun updateAutoSyncEnabled(enabled: Boolean) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(autoSyncEnabled = enabled)
            }
            override suspend fun updateSpreadsheetInfo(id: String, name: String) {
                syncSettingsFlow.value = syncSettingsFlow.value.copy(spreadsheetId = id, spreadsheetName = name)
            }
            override suspend fun recordSyncResult(result: SyncResult) {
                when (result) {
                    is SyncResult.Success -> {
                        syncSettingsFlow.value = syncSettingsFlow.value.copy(
                            lastSyncTimestamp = result.timestamp,
                            lastSyncStatus = "Success (${result.itemsCount} items)",
                            spreadsheetId = result.spreadsheetId
                        )
                    }
                    is SyncResult.Failure -> {
                        syncSettingsFlow.value = syncSettingsFlow.value.copy(
                            lastSyncTimestamp = result.timestamp,
                            lastSyncStatus = "Failed: ${result.error}",
                            isGoogleSignedIn = if (result.isAuthRevoked) false else syncSettingsFlow.value.isGoogleSignedIn
                        )
                    }
                }
            }
        }

        viewModel = SettingsViewModel(
            settingsRepository = mockSettingsRepo,
            tenantRepository = mockTenantRepo,
            propertyRepository = mockPropertyRepo,
            paymentRepository = mockPaymentRepo,
            syncRepository = syncRepository,
            remoteMirror = fakeMirror,
            googleAuthManager = authManager,
            backupExporter = mockk(relaxed = true),
            backupImporter = mockk(relaxed = true),
            isSyncSupported = true
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithGoogle_needsResolution_emitsLaunchAuthResolution() = runTest(testDispatcher) {
        val pendingIntent = mockk<PendingIntent>()
        val mockIntentSender = mockk<android.content.IntentSender>()
        every { pendingIntent.intentSender } returns mockIntentSender

        val authResult = mockk<AuthorizationResult>()
        every { authResult.hasResolution() } returns true
        every { authResult.pendingIntent } returns pendingIntent
        every { authResult.accessToken } returns null
        fakeClient.authorizeTask = Tasks.forResult(authResult)

        val events = mutableListOf<BackupEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.backupEvent.collect { events.add(it) }
        }

        viewModel.signInWithGoogle()
        advanceUntilIdle()

        assertThat(events).hasSize(1)
        val event = events.first() as BackupEvent.LaunchAuthResolution
        assertThat(event.intentSender).isEqualTo(mockIntentSender)
        assertThat(syncSettingsFlow.value.isGoogleSignedIn).isFalse()

        job.cancel()
    }

    @Test
    fun onAuthorizationResult_success_updatesAccountAndEmitsSuccess() = runTest(testDispatcher) {
        val resolvedResult = mockk<AuthorizationResult>()
        every { resolvedResult.hasResolution() } returns false
        every { resolvedResult.accessToken } returns "valid_token_xyz"
        val mockAccount = mockk<com.google.android.gms.auth.api.signin.GoogleSignInAccount>()
        every { mockAccount.email } returns "landlord@gmail.com"
        every { resolvedResult.toGoogleSignInAccount() } returns mockAccount

        fakeClient.intentResult = resolvedResult

        val events = mutableListOf<BackupEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.backupEvent.collect { events.add(it) }
        }

        val mockIntent = Intent()
        viewModel.onAuthorizationResult(Activity.RESULT_OK, mockIntent)
        advanceUntilIdle()

        assertThat(syncSettingsFlow.value.isGoogleSignedIn).isTrue()
        assertThat(syncSettingsFlow.value.userEmail).isEqualTo("landlord@gmail.com")
        assertThat(events).hasSize(1)
        assertThat((events.first() as BackupEvent.Success).message).contains("Connected")

        job.cancel()
    }

    @Test
    fun onAuthorizationResult_userCancelled_emitsErrorAndDoesNotSignIn() = runTest(testDispatcher) {
        val events = mutableListOf<BackupEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.backupEvent.collect { events.add(it) }
        }

        viewModel.onAuthorizationResult(Activity.RESULT_CANCELED, null)
        advanceUntilIdle()

        assertThat(syncSettingsFlow.value.isGoogleSignedIn).isFalse()
        assertThat(events).hasSize(1)
        assertThat((events.first() as BackupEvent.Error).message).contains("cancelled")

        job.cancel()
    }

    @Test
    fun syncNow_whenSignedIn_fetchesFreshTokenAndSyncs() = runTest(testDispatcher) {
        val syncSettingsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.syncSettings.collect {}
        }
        syncSettingsFlow.value = syncSettingsFlow.value.copy(isGoogleSignedIn = true)
        advanceUntilIdle()

        val freshAuthResult = mockk<AuthorizationResult>()
        every { freshAuthResult.hasResolution() } returns false
        every { freshAuthResult.accessToken } returns "fresh_token_789"
        every { freshAuthResult.toGoogleSignInAccount() } returns null
        fakeClient.authorizeTask = Tasks.forResult(freshAuthResult)

        val events = mutableListOf<BackupEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.backupEvent.collect { events.add(it) }
        }

        viewModel.syncNow()
        advanceUntilIdle()

        assertThat(fakeMirror.lastToken).isEqualTo("fresh_token_789")
        assertThat(syncSettingsFlow.value.lastSyncStatus).contains("Success")
        assertThat(events).isNotEmpty()
        assertThat((events.last() as BackupEvent.Success).message).contains("Synced 3 records")

        job.cancel()
        syncSettingsJob.cancel()
    }
}
