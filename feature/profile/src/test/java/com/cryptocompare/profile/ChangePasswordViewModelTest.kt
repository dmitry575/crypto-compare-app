package com.cryptocompare.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.usecase.profile.ChangePasswordUseCase
import com.cryptocompare.profile.util.ChangePasswordError
import com.cryptocompare.profile.viewmodel.changepasswordviewmodel.ChangePasswordViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ChangePasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val changePasswordUseCase = ChangePasswordUseCase(authRepository)

    @Before
    fun setUp() {
        clearMocks(authRepository)
    }

    @Test
    fun `blank current password is rejected before any request`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onNewPasswordChange(NEW_PASSWORD)
            viewModel.onConfirmPasswordChange(NEW_PASSWORD)

            viewModel.changePassword()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.CURRENT_PASSWORD_EMPTY, viewModel.uiState.value.validationError)
            coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
        }

    @Test
    fun `weak new password is rejected`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onCurrentPasswordChange(CURRENT_PASSWORD)
            // нет цифры и коротковат
            viewModel.onNewPasswordChange("abc")
            viewModel.onConfirmPasswordChange("abc")

            viewModel.changePassword()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.NEW_PASSWORD_TOO_WEAK, viewModel.uiState.value.validationError)
            coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
        }

    @Test
    fun `mismatched confirmation is rejected`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onCurrentPasswordChange(CURRENT_PASSWORD)
            viewModel.onNewPasswordChange(NEW_PASSWORD)
            viewModel.onConfirmPasswordChange("secret2b")

            viewModel.changePassword()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.PASSWORDS_DO_NOT_MATCH, viewModel.uiState.value.validationError)
            coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
        }

    @Test
    fun `new password equal to current is rejected`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onCurrentPasswordChange(CURRENT_PASSWORD)
            viewModel.onNewPasswordChange(CURRENT_PASSWORD)
            viewModel.onConfirmPasswordChange(CURRENT_PASSWORD)

            viewModel.changePassword()
            advanceUntilIdle()

            assertEquals(
                ChangePasswordError.NEW_PASSWORD_SAME_AS_CURRENT,
                viewModel.uiState.value.validationError,
            )
            coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
        }

    @Test
    fun `valid form changes the password`() =
        runTest {
            coEvery { authRepository.changePassword(CURRENT_PASSWORD, NEW_PASSWORD) } returns Result.success(Unit)
            val viewModel = createViewModel()
            viewModel.onCurrentPasswordChange(CURRENT_PASSWORD)
            viewModel.onNewPasswordChange(NEW_PASSWORD)
            viewModel.onConfirmPasswordChange(NEW_PASSWORD)

            viewModel.changePassword()
            advanceUntilIdle()

            coVerify(exactly = 1) { authRepository.changePassword(CURRENT_PASSWORD, NEW_PASSWORD) }
            val uiState = viewModel.uiState.value
            assertTrue(uiState.isPasswordChanged)
            assertFalse(uiState.isLoading)
            assertNull(uiState.errorMessage)
            assertNull(uiState.validationError)
        }

    @Test
    fun `failed reauthentication surfaces the firebase message`() =
        runTest {
            coEvery { authRepository.changePassword(any(), any()) } returns
                Result.failure(IllegalStateException(WRONG_PASSWORD_ERROR))
            val viewModel = createViewModel()
            viewModel.onCurrentPasswordChange(CURRENT_PASSWORD)
            viewModel.onNewPasswordChange(NEW_PASSWORD)
            viewModel.onConfirmPasswordChange(NEW_PASSWORD)

            viewModel.changePassword()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(WRONG_PASSWORD_ERROR, uiState.errorMessage)
            assertFalse(uiState.isPasswordChanged)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `password requirements track the new password`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onNewPasswordChange("abc")
            with(viewModel.uiState.value) {
                assertFalse(passwordLengthMet)
                assertTrue(passwordLetterMet)
                assertFalse(passwordNumberMet)
            }

            viewModel.onNewPasswordChange(NEW_PASSWORD)
            with(viewModel.uiState.value) {
                assertTrue(passwordLengthMet)
                assertTrue(passwordLetterMet)
                assertTrue(passwordNumberMet)
            }
        }

    private fun createViewModel(): ChangePasswordViewModel = ChangePasswordViewModel(changePasswordUseCase)

    private companion object {
        const val CURRENT_PASSWORD = "current1a"
        const val NEW_PASSWORD = "secret1b"
        const val WRONG_PASSWORD_ERROR = "The password is invalid or the user does not have a password"
    }
}
