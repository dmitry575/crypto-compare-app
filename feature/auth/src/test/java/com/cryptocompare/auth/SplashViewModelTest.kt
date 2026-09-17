package com.cryptocompare.auth

import com.cryptocompare.auth.viewmodel.splashviewmodel.SplashViewModel
import com.cryptocompare.domain.repository.OnboardingRepository
import com.cryptocompare.domain.usecase.onboarding.HasSeenOnboardingUseCase
import com.cryptocompare.helpers.util.AppConstants.SPLASH_DURATION_MS
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val onboardingRepository: OnboardingRepository = mockk(relaxed = true)
    private val hasSeenOnboardingUseCase = HasSeenOnboardingUseCase(onboardingRepository)

    @Before
    fun setUp() {
        clearMocks(onboardingRepository)
        // по умолчанию онбординг уже показан: запуск идёт прямо в каталог
        coEvery { onboardingRepository.hasSeenOnboarding() } returns true
    }

    @Test
    fun `splash holds the screen until the delay passes`() =
        runTest {
            val viewModel = createViewModel()

            assertTrue(viewModel.uiState.value.isPreparing)

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isPreparing)
        }

    @Test
    fun `first launch asks for onboarding`() =
        runTest {
            coEvery { onboardingRepository.hasSeenOnboarding() } returns false

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertTrue(viewModel.uiState.value.shouldShowOnboarding)
        }

    @Test
    fun `repeat launch skips onboarding`() =
        runTest {
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

            val viewModel = createViewModel()

            advanceTimeBy(SPLASH_DURATION_MS)
            runCurrent()

            assertFalse(viewModel.uiState.value.isPreparing)
            assertFalse(viewModel.uiState.value.shouldShowOnboarding)
        }

    private fun createViewModel(): SplashViewModel = SplashViewModel(hasSeenOnboardingUseCase)
}
