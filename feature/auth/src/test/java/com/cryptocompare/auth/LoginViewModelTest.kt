package com.cryptocompare.auth

import com.cryptocompare.auth.viewmodel.loginviewmodel.LoginViewModel
import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithGoogleUseCase
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
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val isValidEmailUseCase = IsValidEmailUseCase()
    private val signInWithEmailUseCase = SignInWithEmailUseCase(authRepository)
    private val signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository)

    @Test
    fun `signIn with invalid email sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("bad")
            viewModel.onPasswordChange("secret123")

            viewModel.signInWithEmail()

            assertEquals(AppError.Validation(ValidationErrorReason.INVALID_EMAIL), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signInWithEmailUseCase(any(), any()) }
        }

    @Test
    fun `editing the email clears its error, the password does not`() =
        runTest {
            // поле оставалось красным, пока его уже чинят, — до следующего «Войти»
            val viewModel = createViewModel()
            viewModel.onEmailChange("bad")
            viewModel.onPasswordChange("secret123")
            viewModel.signInWithEmail()

            viewModel.onPasswordChange("secret1234")
            assertEquals(AppError.Validation(ValidationErrorReason.INVALID_EMAIL), viewModel.uiState.value.error)

            viewModel.onEmailChange("bad@")
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `editing a field keeps a sign-in failure on screen`() =
        runTest {
            // неверный пароль — не ошибка проверки поля: правка почты его не исправила
            coEvery { signInWithEmailUseCase(any(), any()) } returns
                Result.failure(AppException(AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS)))
            val viewModel = createViewModel()
            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("secret123")
            viewModel.signInWithEmail()
            advanceUntilIdle()

            viewModel.onEmailChange("user2@example.com")

            assertEquals(AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS), viewModel.uiState.value.error)
        }

    @Test
    fun `signIn with short password sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("123")

            viewModel.signInWithEmail()

            assertEquals(AppError.Validation(ValidationErrorReason.PASSWORD_TOO_SHORT), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signInWithEmailUseCase(any(), any()) }
        }

    @Test
    fun `signIn success clears loading`() =
        runTest {
            coEvery { signInWithEmailUseCase(any(), any()) } returns Result.success(mockk())
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("secret123")

            viewModel.signInWithEmail()
            assertEquals(true, viewModel.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `signIn failure sets error`() =
        runTest {
            coEvery { signInWithEmailUseCase(any(), any()) } returns
                Result.failure(AppException(AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS)))
            val viewModel = createViewModel()

            viewModel.onEmailChange("user@example.com")
            viewModel.onPasswordChange("secret123")

            viewModel.signInWithEmail()
            advanceUntilIdle()

            assertEquals(AppError.Auth(AuthErrorReason.INVALID_CREDENTIALS), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `signInWithGoogle blank token sets error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.signInWithGoogle("")

            assertEquals(AppError.Auth(AuthErrorReason.GOOGLE_TOKEN_MISSING), viewModel.uiState.value.error)
            coVerify(exactly = 0) { signInWithGoogleUseCase(any()) }
        }

    @Test
    fun `signInWithGoogle success clears loading`() =
        runTest {
            coEvery { signInWithGoogleUseCase(any()) } returns Result.success(mockk())
            val viewModel = createViewModel()

            viewModel.signInWithGoogle("token")
            assertEquals(true, viewModel.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    private fun createViewModel(): LoginViewModel =
        LoginViewModel(
            isValidEmailUseCase = isValidEmailUseCase,
            signInWithEmailUseCase = signInWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
        )
}
