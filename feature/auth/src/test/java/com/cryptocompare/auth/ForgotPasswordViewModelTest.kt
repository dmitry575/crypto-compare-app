package com.cryptocompare.auth

import com.cryptocompare.auth.viewmodel.forgotpasswordviewmodel.ForgotPasswordViewModel
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.ValidationErrorReason
import com.cryptocompare.testing.MainDispatcherRule
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val isValidEmailUseCase = IsValidEmailUseCase()
    private val sendPasswordResetEmailUseCase = SendPasswordResetEmailUseCase(authRepository)

    @Test
    fun `invalid email sets error and sends nothing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("bad")
            viewModel.sendResetEmail()
            advanceUntilIdle()

            assertEquals(AppError.Validation(ValidationErrorReason.INVALID_EMAIL), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isEmailSent)
            coVerify(exactly = 0) { authRepository.sendPasswordResetEmail(any()) }
        }

    @Test
    fun `blank email sets error and sends nothing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.sendResetEmail()
            advanceUntilIdle()

            assertEquals(AppError.Validation(ValidationErrorReason.INVALID_EMAIL), viewModel.uiState.value.error)
            coVerify(exactly = 0) { authRepository.sendPasswordResetEmail(any()) }
        }

    @Test
    fun `valid email sends the reset link and reports success`() =
        runTest {
            coEvery { authRepository.sendPasswordResetEmail(EMAIL) } returns Result.success(Unit)
            val viewModel = createViewModel()

            viewModel.onEmailChange(EMAIL)
            viewModel.sendResetEmail()
            advanceUntilIdle()

            coVerify(exactly = 1) { authRepository.sendPasswordResetEmail(EMAIL) }
            val uiState = viewModel.uiState.value
            assertTrue(uiState.isEmailSent)
            assertFalse(uiState.isLoading)
            assertNull(uiState.error)
        }

    @Test
    fun `surrounding whitespace is trimmed before sending`() =
        runTest {
            coEvery { authRepository.sendPasswordResetEmail(EMAIL) } returns Result.success(Unit)
            val viewModel = createViewModel()

            viewModel.onEmailChange("  $EMAIL  ")
            viewModel.sendResetEmail()
            advanceUntilIdle()

            coVerify(exactly = 1) { authRepository.sendPasswordResetEmail(EMAIL) }
        }

    @Test
    fun `failure keeps the form and shows the firebase message`() =
        runTest {
            coEvery { authRepository.sendPasswordResetEmail(any()) } returns
                Result.failure(AppException(AppError.Network))
            val viewModel = createViewModel()

            viewModel.onEmailChange(EMAIL)
            viewModel.sendResetEmail()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(AppError.Network, uiState.error)
            assertFalse(uiState.isEmailSent)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `retry after a failure clears the previous error`() =
        runTest {
            coEvery { authRepository.sendPasswordResetEmail(EMAIL) } returns
                Result.failure(AppException(AppError.Network)) andThen Result.success(Unit)
            val viewModel = createViewModel()

            viewModel.onEmailChange(EMAIL)
            viewModel.sendResetEmail()
            advanceUntilIdle()
            assertEquals(AppError.Network, viewModel.uiState.value.error)

            viewModel.sendResetEmail()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.isEmailSent)
        }

    private fun createViewModel(): ForgotPasswordViewModel =
        ForgotPasswordViewModel(sendPasswordResetEmailUseCase, isValidEmailUseCase)

    private companion object {
        const val EMAIL = "user@example.com"
    }
}
