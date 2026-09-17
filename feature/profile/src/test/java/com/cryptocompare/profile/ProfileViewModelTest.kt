package com.cryptocompare.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import com.cryptocompare.domain.repository.LanguageRepository
import com.cryptocompare.domain.repository.ThemeRepository
import com.cryptocompare.domain.usecase.auth.ObserveAuthStateUseCase
import com.cryptocompare.domain.usecase.profile.DeleteAccountUseCase
import com.cryptocompare.domain.usecase.profile.SignOutUseCase
import com.cryptocompare.domain.usecase.settings.ObserveLanguageUseCase
import com.cryptocompare.domain.usecase.settings.ObserveThemePreferenceUseCase
import com.cryptocompare.domain.usecase.settings.SetLanguageUseCase
import com.cryptocompare.domain.usecase.settings.SetThemePreferenceUseCase
import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.model.settings.AppLanguage
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

    @Before
    fun setUp() {
        clearMocks(authRepository, themeRepository, languageRepository)
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

    private fun createViewModel(): ProfileViewModel =
        ProfileViewModel(
            observeAuthStateUseCase = observeAuthStateUseCase,
            signOutUseCase = signOutUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
            setThemePreferenceUseCase = setThemePreferenceUseCase,
            setLanguageUseCase = setLanguageUseCase,
            observeThemePreferenceUseCase = observeThemePreferenceUseCase,
            observeLanguageUseCase = observeLanguageUseCase,
        )

    private companion object {
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
