package com.cryptocompare.auth

import com.cryptocompare.auth.viewmodel.registrationviewmodel.RegistrationViewModel
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithGoogleUseCase
import com.cryptocompare.domain.usecase.auth.SignUpWithEmailUseCase
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.AuthErrorReason
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val isValidEmailUseCase = IsValidEmailUseCase()
    private val signUpWithEmailUseCase = SignUpWithEmailUseCase(authRepository)
    private val signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository)

    @Test
    fun `signUp with invalid email sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("bad")
            viewModel.onPasswordChange("Password1")
            viewModel.onConfirmPasswordChange("Password1")

            viewModel.signUpWithEmail()

            assertEquals(AppError.Validation(ValidationErrorReason.INVALID_EMAIL), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signUpWithEmailUseCase(any(), any()) }
        }

    @Test
    fun `signUp with weak password sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("short")
            viewModel.onConfirmPasswordChange("short")

            viewModel.signUpWithEmail()

            assertEquals(AppError.Validation(ValidationErrorReason.PASSWORD_TOO_WEAK), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signUpWithEmailUseCase(any(), any()) }
        }

    @Test
    fun `signUp with mismatched password sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("Password1")
            viewModel.onConfirmPasswordChange("Password2")

            viewModel.signUpWithEmail()

            assertEquals(
                AppError.Validation(ValidationErrorReason.PASSWORDS_DO_NOT_MATCH),
                viewModel.uiState.value.error,
            )
            coVerify(exactly = 0) { signUpWithEmailUseCase(any(), any()) }
        }

    @Test
    fun `a mismatch clears when either password field is edited`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("Password1")
            viewModel.onConfirmPasswordChange("Password2")
            viewModel.signUpWithEmail()

            viewModel.onEmailChange("user2@example.com")
            assertEquals(
                AppError.Validation(ValidationErrorReason.PASSWORDS_DO_NOT_MATCH),
                viewModel.uiState.value.error,
            )

            viewModel.onPasswordChange("Password2")
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `signUp success clears loading`() =
        runTest {
            coEvery { signUpWithEmailUseCase(any(), any()) } returns Result.success(mockk())
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("Password1")
            viewModel.onConfirmPasswordChange("Password1")

            viewModel.signUpWithEmail()
            assertEquals(true, viewModel.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `signUp failure sets error`() =
        runTest {
            coEvery { signUpWithEmailUseCase(any(), any()) } returns
                Result.failure(AppException(AppError.Auth(AuthErrorReason.EMAIL_ALREADY_IN_USE)))
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("Password1")
            viewModel.onConfirmPasswordChange("Password1")

            viewModel.signUpWithEmail()
            advanceUntilIdle()

            assertEquals(AppError.Auth(AuthErrorReason.EMAIL_ALREADY_IN_USE), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `signUpWithGoogle blank token sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.signUpWithGoogle("")

            assertEquals(AppError.Auth(AuthErrorReason.GOOGLE_TOKEN_MISSING), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signInWithGoogleUseCase(any()) }
        }

    @Test
    fun `signUpWithGoogle success clears loading`() =
        runTest {
            coEvery { signInWithGoogleUseCase(any()) } returns Result.success(mockk())
            val viewModel = createViewModel()

            viewModel.signUpWithGoogle("token")
            assertEquals(true, viewModel.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    private fun createViewModel(): RegistrationViewModel =
        RegistrationViewModel(
            isValidEmailUseCase = isValidEmailUseCase,
            signUpWithEmailUseCase = signUpWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
        )
}
