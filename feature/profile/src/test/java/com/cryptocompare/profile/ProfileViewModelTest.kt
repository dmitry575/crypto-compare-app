package com.cryptocompare.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.domain.usecase.auth.ObserveAuthStateUseCase
import com.cryptocompare.domain.usecase.pairs.GetProvidersUseCase
import com.cryptocompare.domain.usecase.profile.DeleteAccountUseCase
import com.cryptocompare.domain.usecase.profile.SignOutUseCase
import com.cryptocompare.domain.usecase.settings.ObserveLanguageUseCase
import com.cryptocompare.domain.usecase.settings.ObserveMarketPreferencesUseCase
import com.cryptocompare.domain.usecase.settings.ObserveThemePreferenceUseCase
import com.cryptocompare.domain.usecase.settings.SetDefaultProviderUseCase
import com.cryptocompare.domain.usecase.settings.SetDefaultTimeframeUseCase
import com.cryptocompare.domain.usecase.settings.SetLanguageUseCase
import com.cryptocompare.domain.usecase.settings.SetThemePreferenceUseCase
import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.settings.AppLanguage
import com.cryptocompare.model.settings.MarketPreferences
import com.cryptocompare.model.settings.ThemePreference
import com.cryptocompare.profile.viewmodel.profileviewmodel.ProfileViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository)
    private val signOutUseCase = SignOutUseCase(authRepository)
    private val favouriteSymbolRepository: FavouriteSymbolRepository = mockk(relaxed = true)
    private val deleteAccountUseCase = DeleteAccountUseCase(authRepository, favouriteSymbolRepository)
    private val themeRepository: ThemeRepository = mockk(relaxed = true)
    private val setThemePreferenceUseCase = SetThemePreferenceUseCase(themeRepository)
    private val observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(themeRepository)
    private val languageRepository: LanguageRepository = mockk(relaxed = true)
    private val setLanguageUseCase = SetLanguageUseCase(languageRepository)
    private val observeLanguageUseCase = ObserveLanguageUseCase(languageRepository)
    private val marketPreferencesRepository: MarketPreferencesRepository = mockk(relaxed = true)
    private val observeMarketPreferencesUseCase = ObserveMarketPreferencesUseCase(marketPreferencesRepository)
    private val setDefaultProviderUseCase = SetDefaultProviderUseCase(marketPreferencesRepository)
    private val setDefaultTimeframeUseCase = SetDefaultTimeframeUseCase(marketPreferencesRepository)
    private val cryptoCompareRepository: CryptoCompareRepository = mockk(relaxed = true)
    private val getProvidersUseCase = GetProvidersUseCase(cryptoCompareRepository)

    @Before
    fun setUp() {
        clearMocks(
            authRepository,
            themeRepository,
            languageRepository,
            marketPreferencesRepository,
            cryptoCompareRepository,
        )
        every { marketPreferencesRepository.observeMarketPreferences() } returns flowOf(MarketPreferences())
        coEvery { cryptoCompareRepository.getProviders() } returns Result.success(PROVIDERS)
        every { authRepository.observeAuthState() } returns flowOf(TEST_USER)
        every { themeRepository.observeThemePreference() } returns flowOf(ThemePreference.SYSTEM)
        every { languageRepository.observeLanguage() } returns flowOf(AppLanguage.SYSTEM)
    }

    @Test
    fun `init exposes current user`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(TEST_USER, viewModel.uiState.value.user)
        }

    @Test
    fun `without a session the screen shows a guest`() =
        runTest {
            every { authRepository.observeAuthState() } returns flowOf(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            // гостю показывается приглашение войти вместо раздела аккаунта
            assertNull(viewModel.uiState.value.user)
        }

    @Test
    fun `sign out confirmation is asked before signing out`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onSignOutClick()

            assertTrue(viewModel.uiState.value.showSignOutConfirmation)
            coVerify(exactly = 0) { authRepository.signOut() }
        }

    @Test
    fun `confirmed sign out calls use case and closes the dialog`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onSignOutClick()
            viewModel.onSignOutConfirmed()
            advanceUntilIdle()

            coVerify(exactly = 1) { authRepository.signOut() }
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertFalse(uiState.showSignOutConfirmation)
            assertNull(uiState.errorMessage)
        }

    @Test
    fun `confirmed delete account clears state on success`() =
        runTest {
            coEvery { favouriteSymbolRepository.deleteAllFavourites() } returns Result.success(Unit)
            coEvery { authRepository.deleteAccount() } returns Result.success(Unit)
            val viewModel = createViewModel()

            viewModel.onDeleteAccountClick()
            viewModel.onDeleteAccountConfirmed()
            advanceUntilIdle()

            coVerify(exactly = 1) { favouriteSymbolRepository.deleteAllFavourites() }
            coVerify(exactly = 1) { authRepository.deleteAccount() }
            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertFalse(uiState.showDeleteConfirmation)
            assertNull(uiState.errorMessage)
        }

    @Test
    fun `delete account failure keeps user and shows error`() =
        runTest {
            coEvery { favouriteSymbolRepository.deleteAllFavourites() } returns Result.success(Unit)
            coEvery { authRepository.deleteAccount() } returns
                Result.failure(IllegalStateException(RECENT_LOGIN_ERROR))
            val viewModel = createViewModel()

            viewModel.onDeleteAccountClick()
            viewModel.onDeleteAccountConfirmed()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(TEST_USER, uiState.user)
            assertFalse(uiState.isLoading)
            assertEquals(RECENT_LOGIN_ERROR, uiState.errorMessage)
        }

    @Test
    fun `dismissed delete confirmation does not delete anything`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDeleteAccountClick()
            viewModel.onDeleteAccountDismissed()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showDeleteConfirmation)
            coVerify(exactly = 0) { authRepository.deleteAccount() }
        }

    @Test
    fun `stored theme preference reaches the ui state`() =
        runTest {
            every { themeRepository.observeThemePreference() } returns flowOf(ThemePreference.DARK)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(ThemePreference.DARK, viewModel.uiState.value.themePreference)
        }

    @Test
    fun `choosing a theme is persisted`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onThemePreferenceChange(ThemePreference.LIGHT)
            advanceUntilIdle()

            coVerify(exactly = 1) { themeRepository.setThemePreference(ThemePreference.LIGHT) }
        }

    @Test
    fun `market preferences reach the ui state`() =
        runTest {
            every { marketPreferencesRepository.observeMarketPreferences() } returns
                flowOf(MarketPreferences(defaultProviderId = 19, timeframe = ChartTimeframe.H4))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(19, viewModel.uiState.value.marketPreferences.defaultProviderId)
            assertEquals(ChartTimeframe.H4, viewModel.uiState.value.marketPreferences.timeframe)
        }

    @Test
    fun `the exchange list is loaded for the picker`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(PROVIDERS, viewModel.uiState.value.providers)
        }

    @Test
    fun `choosing an exchange stores it and closes the picker`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDefaultExchangeClick()
            assertTrue(viewModel.uiState.value.showExchangePicker)

            viewModel.onDefaultExchangeSelected(19)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showExchangePicker)
            coVerify(exactly = 1) { marketPreferencesRepository.setDefaultProvider(19) }
        }

    @Test
    fun `choosing the first available exchange clears the stored one`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onDefaultExchangeSelected(null)
            advanceUntilIdle()

            coVerify(exactly = 1) { marketPreferencesRepository.setDefaultProvider(null) }
        }

    @Test
    fun `choosing a timeframe is persisted`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onDefaultTimeframeChange(ChartTimeframe.M15)
            advanceUntilIdle()

            coVerify(exactly = 1) { marketPreferencesRepository.setDefaultTimeframe(ChartTimeframe.M15) }
        }

    private fun createViewModel(): ProfileViewModel =
        ProfileViewModel(
            observeAuthStateUseCase = observeAuthStateUseCase,
            signOutUseCase = signOutUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
            setThemePreferenceUseCase = setThemePreferenceUseCase,
            setLanguageUseCase = setLanguageUseCase,
            observeThemePreferenceUseCase = observeThemePreferenceUseCase,
            observeLanguageUseCase = observeLanguageUseCase,
            observeMarketPreferencesUseCase = observeMarketPreferencesUseCase,
            setDefaultProviderUseCase = setDefaultProviderUseCase,
            setDefaultTimeframeUseCase = setDefaultTimeframeUseCase,
            getProvidersUseCase = getProvidersUseCase,
        )

    private companion object {
        val PROVIDERS =
            listOf(
                Provider(id = 1, name = "mexc", referralUrl = null, status = ProviderStatus.Enabled),
                Provider(id = 19, name = "binance", referralUrl = null, status = ProviderStatus.Enabled),
            )

        const val RECENT_LOGIN_ERROR = "This operation requires recent authentication"

        val TEST_USER =
            AuthUser(
                uid = "uid",
                email = "test@example.com",
                displayName = "Test",
                photoUrl = null,
                hasPasswordProvider = true,
            )
    }
}
