package com.tbterminal.app.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.auth.DefaultPinUnlockService
import com.tbterminal.app.data.auth.InMemoryOfflinePinVerifierStore
import com.tbterminal.app.data.model.AuthenticatedSession
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.AuthApi
import com.tbterminal.app.data.remote.ChangePasswordRequestDto
import com.tbterminal.app.data.remote.ChangePinRequestDto
import com.tbterminal.app.data.remote.CurrentUserDto
import com.tbterminal.app.data.remote.LoginRequest
import com.tbterminal.app.data.remote.LoginResponse
import com.tbterminal.app.data.remote.PinUnlockResponse
import com.tbterminal.app.data.remote.ResetPinRequestDto
import com.tbterminal.app.data.remote.UnlockRequest
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RemoteAuthRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.data.session.SessionUser
import com.tbterminal.app.data.session.TokenStore
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.net.ConnectException
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class PinScreenBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun keypadEntryBackspaceAndAutoCompleteWork() {
        var state by mutableStateOf(PinUiState())
        var completedPin = ""
        var switched = false

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    state = state,
                    onDigit = { d -> state = state.copy(pin = state.pin + d, error = null) },
                    onBackspace = { state = state.copy(pin = state.pin.dropLast(1), error = null) },
                    onComplete = { completedPin = it },
                    onSwitchAccount = { switched = true }
                )
            }
        }

        composeRule.onNodeWithTag("pin-key-1").performClick()
        composeRule.onNodeWithTag("pin-key-2").performClick()
        composeRule.onNodeWithContentDescription("2 dari 6 digit terisi").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Hapus digit").performClick()
        composeRule.onNodeWithContentDescription("1 dari 6 digit terisi").assertIsDisplayed()

        listOf("2", "3", "4", "5", "6").forEach { d ->
            composeRule.onNodeWithTag("pin-key-$d").performClick()
        }
        assertEquals("123456", completedPin)

        composeRule.onNodeWithText("Masukkan PIN").assertDoesNotExist()
        composeRule.onNodeWithText("Masukkan 6 digit PIN untuk otentikasi kasir").assertDoesNotExist()
        composeRule.onNodeWithText("Gunakan akun lain").assertDoesNotExist()

        val backBounds = composeRule.onNodeWithTag("pin-back-button").getBoundsInRoot()
        val chipBounds = composeRule.onNodeWithTag("pin-user-chip").getBoundsInRoot()
        val dotsBounds = composeRule.onNodeWithTag("pin-dots-row").getBoundsInRoot()
        assertTrue(backBounds.bottom < dotsBounds.top)
        assertTrue(backBounds.left < 40.dp)
        assertTrue(chipBounds.left > backBounds.right)
        assertEquals(backBounds.top.value, chipBounds.top.value, 2f)
        assertTrue(chipBounds.bottom < dotsBounds.top)

        composeRule.onNodeWithTag("pin-back-button").performClick()
        assertTrue(switched)
        composeRule.onNodeWithText("Terminal · v1.0.0").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun errorMessageDoesNotShiftKeypadAndHasProperSemantics() {
        var state by mutableStateOf(PinUiState())

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    state = state,
                    onDigit = {},
                    onBackspace = {},
                    onComplete = {},
                    onSwitchAccount = {}
                )
            }
        }

        val initialKey1Bounds = composeRule.onNodeWithTag("pin-key-1").getBoundsInRoot()
        composeRule.onNodeWithTag("pin-dots-row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        state = state.copy(error = "PIN salah. Silakan coba lagi.")
        composeRule.waitForIdle()

        val errorKey1Bounds = composeRule.onNodeWithTag("pin-key-1").getBoundsInRoot()
        assertEquals(initialKey1Bounds.top, errorKey1Bounds.top)
        composeRule.onNodeWithText("PIN salah. Silakan coba lagi.").assertIsDisplayed()
        composeRule.onNodeWithTag("pin-error-text")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun forgotPinIsPositionedBelowDotsAndAboveKeypad() {
        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    state = PinUiState(),
                    onDigit = {},
                    onBackspace = {},
                    onComplete = {},
                    onSwitchAccount = {},
                    onForgotPin = {}
                )
            }
        }

        val chipBounds = composeRule.onNodeWithTag("pin-user-chip").getBoundsInRoot()
        val dotsBounds = composeRule.onNodeWithTag("pin-dots-row").getBoundsInRoot()
        val forgotBounds = composeRule.onNodeWithTag("pin-forgot-pin").getBoundsInRoot()
        val sheetBounds = composeRule.onNodeWithTag("pin-keypad-sheet").getBoundsInRoot()
        val key1Bounds = composeRule.onNodeWithTag("pin-key-1").getBoundsInRoot()

        assertTrue(chipBounds.bottom <= sheetBounds.top)
        assertTrue(dotsBounds.top >= sheetBounds.top)
        assertTrue(forgotBounds.top >= dotsBounds.bottom)
        assertTrue(forgotBounds.bottom <= key1Bounds.top)
        assertTrue(key1Bounds.top >= sheetBounds.top)
        composeRule.onNodeWithText("Lupa PIN?").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun tabletUsesTwoColumnLayoutWithHeroOnLeftAndKeypadOnRight() {
        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    state = PinUiState(),
                    onDigit = {},
                    onBackspace = {},
                    onComplete = {},
                    onSwitchAccount = {}
                )
            }
        }

        val backBounds = composeRule.onNodeWithTag("pin-back-button").getBoundsInRoot()
        val chipBounds = composeRule.onNodeWithTag("pin-user-chip").getBoundsInRoot()
        val sheetBounds = composeRule.onNodeWithTag("pin-keypad-sheet").getBoundsInRoot()
        val dotsBounds = composeRule.onNodeWithTag("pin-dots-row").getBoundsInRoot()
        val key1Bounds = composeRule.onNodeWithTag("pin-key-1").getBoundsInRoot()
        val key3Bounds = composeRule.onNodeWithTag("pin-key-3").getBoundsInRoot()

        assertEquals(backBounds.top.value, chipBounds.top.value, 2f)
        assertTrue(chipBounds.right <= sheetBounds.left + 2.dp)
        assertTrue(dotsBounds.left >= sheetBounds.left)
        val keypadWidth = key3Bounds.right - key1Bounds.left
        // Tablet input panel max width 334dp minus horizontal padding (28*2 = 56dp) or 334dp
        assertTrue(keypadWidth.value in 275f..336f)
        composeRule.onNodeWithText("Barokah Jaya").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun lockoutDisablesKeypadShowsCountdownAndKeepsForgotPinEnabled() {
        var digitClicks = 0
        var forgotClicks = 0

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    state = PinUiState(
                        error = "Terlalu banyak percobaan · 00:30",
                        isLockedOut = true,
                        lockoutSecondsRemaining = 30L
                    ),
                    onDigit = { digitClicks++ },
                    onBackspace = { digitClicks++ },
                    onComplete = {},
                    onSwitchAccount = {},
                    onForgotPin = { forgotClicks++ }
                )
            }
        }

        composeRule.onNodeWithText("Terlalu banyak percobaan · 00:30").assertIsDisplayed()
        composeRule.onNodeWithTag("pin-key-1").performClick()
        composeRule.onNodeWithTag("pin-key-backspace").performClick()
        assertEquals(0, digitClicks)

        composeRule.onNodeWithTag("pin-forgot-pin").performClick()
        assertEquals(1, forgotClicks)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun wrongPinErrorAutoDismissesAfterThreeSeconds() {
        val fakeRepo = object : AuthRepository {
            override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> =
                error("unused")

            override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> =
                RepositoryResult.Success(PinUnlockResult.Failed(message = "PIN salah", attemptsLeft = 4))

            override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> =
                error("unused")

            override suspend fun getProfile(): RepositoryResult<UserProfile> = error("unused")
            override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> =
                error("unused")

            override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> = error("unused")
        }
        val viewModel = AuthViewModel(fakeRepo)

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    viewModel = viewModel,
                    userName = "Pemilik Toko",
                    onUnlockSuccess = {},
                    onBackToLogin = {}
                )
            }
        }

        listOf("9", "9", "9", "9", "9", "9").forEach { d ->
            composeRule.onNodeWithTag("pin-key-$d").performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PIN salah. Silakan coba lagi.").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(3100L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("PIN salah. Silakan coba lagi.").assertDoesNotExist()
        assertEquals(UnlockState.Idle, viewModel.unlockState.value)
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun offlineWithoutVerifierShowsButuhKoneksiVerbatimInsteadOfGenericNetworkAlert() {
        val fakeRepo = object : AuthRepository {
            override suspend fun login(username: String, password: String): RepositoryResult<AuthenticatedSession> =
                error("unused")

            override suspend fun unlock(pin: String): RepositoryResult<PinUnlockResult> =
                RepositoryResult.Error("OFFLINE_VERIFIER_UNAVAILABLE", "Butuh koneksi untuk membuka kunci")

            override suspend fun resetPinWithPassword(currentPassword: String, newPin: String): RepositoryResult<Unit> =
                error("unused")

            override suspend fun getProfile(): RepositoryResult<UserProfile> = error("unused")
            override suspend fun changeMyPassword(oldPassword: String, newPassword: String): RepositoryResult<Unit> =
                error("unused")

            override suspend fun changeMyPin(oldPin: String, newPin: String): RepositoryResult<Unit> = error("unused")
        }
        val viewModel = AuthViewModel(fakeRepo)

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    viewModel = viewModel,
                    userName = "Kasir",
                    onUnlockSuccess = {},
                    onBackToLogin = {}
                )
            }
        }

        listOf("1", "2", "3", "4", "5", "6").forEach { d ->
            composeRule.onNodeWithTag("pin-key-$d").performClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Butuh koneksi untuk membuka kunci").assertIsDisplayed()
        composeRule.onNodeWithText("Periksa koneksi internet.").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun offlineWrongPinFiveTimesShowsLockoutCountdownAndDisablesKeypad() {
        var wallMillis = 1_800_000_000_000L
        var elapsedMillis = 50_000L
        var unlockApiCalls = 0
        val tokenStore = object : TokenStore {
            private var access: String? = "access-token"
            private var refresh: String? = "refresh-token"
            private var user: SessionUser? = SessionUser("Kasir", "kasir", true, "2026-01-01", null, userId = "kasir-ui")
            override fun readAccessToken(): String? = access
            override fun readRefreshToken(): String? = refresh
            override fun saveAccessToken(token: String) { access = token }
            override fun saveTokens(accessToken: String, refreshToken: String) {
                access = accessToken
                refresh = refreshToken
            }
            override fun readSessionUser(): SessionUser? = user
            override fun saveSessionUser(user: SessionUser) { this.user = user }
            override fun clearAccessToken() {
                access = null
                refresh = null
                user = null
            }
        }
        val sessionManager = SessionManager(tokenStore).apply { lockForResume() }
        val verifierStore = InMemoryOfflinePinVerifierStore(pbkdf2Iterations = 10)
        verifierStore.saveVerifier(
            userId = "kasir-ui",
            pin = "123456",
            verifiedAtMillis = wallMillis,
            verifiedAtElapsedMillis = elapsedMillis,
            verifiedAtBootCount = 1L,
        )
        val offlineApi = object : AuthApi {
            override suspend fun login(request: LoginRequest): Response<ApiResponse<LoginResponse>> = error("unused")
            override suspend fun unlock(request: UnlockRequest): Response<ApiResponse<PinUnlockResponse>> {
                unlockApiCalls++
                throw ConnectException("offline")
            }
            override suspend fun resetPinWithPassword(request: ResetPinRequestDto): Response<ApiResponse<Unit>> = error("unused")
            override suspend fun getMe(): Response<ApiResponse<CurrentUserDto>> = error("unused")
            override suspend fun changeMyPassword(request: ChangePasswordRequestDto): Response<ApiResponse<Unit>> = error("unused")
            override suspend fun changeMyPin(request: ChangePinRequestDto): Response<ApiResponse<Unit>> = error("unused")
        }
        val pinUnlockService = DefaultPinUnlockService(
            authApiProvider = { offlineApi },
            sessionManager = sessionManager,
            verifierStore = verifierStore,
            ioDispatcher = Dispatchers.Main.immediate,
            elapsedRealtimeMillis = { elapsedMillis },
            bootCountProvider = { 1L },
            clockMillis = { wallMillis },
        )
        val repo = RemoteAuthRepository(
            sessionManager = sessionManager,
            authApiProvider = { offlineApi },
            ioDispatcher = Dispatchers.Main.immediate,
            clockMillis = { wallMillis },
            pinUnlockService = pinUnlockService,
        )
        val viewModel = AuthViewModel(
            authRepository = repo,
            clockMillis = { wallMillis },
            tickIntervalMillis = 1_000L,
            elapsedRealtimeMillis = { elapsedMillis },
        )

        composeRule.setContent {
            TbterminalappTheme {
                PinScreen(
                    viewModel = viewModel,
                    userName = "Kasir",
                    onUnlockSuccess = {},
                    onBackToLogin = {},
                )
            }
        }

        // Submit wrong PIN 5 times via UI keypad
        repeat(5) {
            repeat(6) {
                composeRule.onNodeWithTag("pin-key-9").performClick()
            }
            composeRule.waitForIdle()
        }

        assertEquals(5, unlockApiCalls)
        assertTrue(viewModel.unlockState.value is UnlockState.LockedOut)
        composeRule.onNodeWithText("00:30", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("pin-key-1").assertIsNotEnabled()
        composeRule.onNodeWithTag("pin-key-backspace").assertIsNotEnabled()

        // Tapping keypad while locked does not enter digits or trigger unlock
        composeRule.onNodeWithTag("pin-key-1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("0 dari 6 digit terisi").assertIsDisplayed()
        assertEquals(5, unlockApiCalls)
        viewModel.forceClearUnlockState()
    }
}
