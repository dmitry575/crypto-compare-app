package com.cryptocompare.auth

import com.cryptocompare.auth.viewmodel.splashviewmodel.SplashViewModel
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.OnboardingRepository
import com.cryptocompare.domain.usecase.auth.GetCurrentUserUseCase
import com.cryptocompare.domain.usecase.onboarding.HasSeenOnboardingUseCase
import com.cryptocompare.helpers.util.AppConstants.SPLASH_DURATION_MS
import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)

    private val onboardingRepository: OnboardingRepository = mockk(relaxed = true)
    private val hasSeenOnboardingUseCase = HasSeenOnboardingUseCase(onboardingRepository)

    @Before
    fun setUp() {
        clearMocks(authRepository, onboardingRepository)
        // по умолчанию онбординг уже показан: старые кейсы про него не знают
        coEvery { onboardingRepository.hasSeenOnboarding() } returns true
    }

    @Test
    fun `init with authorized user sets authenticated true after splash delay`() =
        runTest {
            every { getCurrentUserUseCase() } returns TEST_USER

            val viewModel = createViewModel()

            assertTrue(viewModel.uiState.value.isCheckAuth)

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isCheckAuth)
            assertEquals(true, viewModel.uiState.value.isAuthenticated)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `init with no user sets authenticated false after splash delay`() =
        runTest {
            every { getCurrentUserUseCase() } returns null

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isCheckAuth)
            assertEquals(false, viewModel.uiState.value.isAuthenticated)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `init auth check failure exposes error and unauthenticated state`() =
        runTest {
            every { getCurrentUserUseCase() } throws IllegalStateException("network down")

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isCheckAuth)
            assertEquals(false, viewModel.uiState.value.isAuthenticated)
            assertEquals("network down", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `retry after failure succeeds and clears error`() =
        runTest {
            every { getCurrentUserUseCase() } throws IllegalStateException("network down") andThen TEST_USER

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()
            assertEquals("network down", viewModel.uiState.value.errorMessage)

            viewModel.checkAuthentification()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isCheckAuth)
            assertEquals(true, viewModel.uiState.value.isAuthenticated)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `first launch asks for onboarding regardless of auth state`() =
        runTest {
            coEvery { onboardingRepository.hasSeenOnboarding() } returns false
            every { getCurrentUserUseCase() } returns TEST_USER

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            // вошедший пользователь тоже видит онбординг: он про продукт, не про вход
            assertTrue(viewModel.uiState.value.shouldShowOnboarding)
            assertEquals(true, viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun `first launch without a user still asks for onboarding`() =
        runTest {
            coEvery { onboardingRepository.hasSeenOnboarding() } returns false
            every { getCurrentUserUseCase() } returns null

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertTrue(viewModel.uiState.value.shouldShowOnboarding)
            assertEquals(false, viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun `repeat launch skips onboarding`() =
        runTest {
            coEvery { onboardingRepository.hasSeenOnboarding() } returns true
            every { getCurrentUserUseCase() } returns TEST_USER

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.shouldShowOnboarding)
        }

    @Test
    fun `unreadable flag does not block the launch`() =
        runTest {
            // застрять на сплеше хуже, чем показать онбординг второй раз
            coEvery { onboardingRepository.hasSeenOnboarding() } throws IllegalStateException("datastore down")
            every { getCurrentUserUseCase() } returns TEST_USER

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isCheckAuth)
            assertFalse(viewModel.uiState.value.shouldShowOnboarding)
            assertEquals(true, viewModel.uiState.value.isAuthenticated)
        }

    @Test
    fun `onboarding is offered even when the auth check fails`() =
        runTest {
            coEvery { onboardingRepository.hasSeenOnboarding() } returns false
            every { getCurrentUserUseCase() } throws IllegalStateException("network down")

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            // онбординг не требует сети, показать его можно и без проверки входа
            assertTrue(viewModel.uiState.value.shouldShowOnboarding)
            assertEquals("network down", viewModel.uiState.value.errorMessage)
        }

    private fun createViewModel(): SplashViewModel = SplashViewModel(getCurrentUserUseCase, hasSeenOnboardingUseCase)

    private companion object {
        val TEST_USER =
            AuthUser(
                uid = "uid",
                email = "test@example.com",
                displayName = "Test",
                photoUrl = null,
            )
    }
}
