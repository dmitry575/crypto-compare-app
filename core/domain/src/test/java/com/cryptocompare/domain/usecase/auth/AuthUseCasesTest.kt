package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Use case'ы авторизации — тонкие обёртки над репозиторием. Тесты фиксируют
 * ровно это: аргументы доходят без изменений, результат не переписывается.
 */
class AuthUseCasesTest {
    private val repository: AuthRepository = mockk()

    @Test
    fun `GetCurrentUserUseCase returns the signed in user`() {
        every { repository.currentUser } returns USER

        assertEquals(USER, GetCurrentUserUseCase(repository)())
        verify(exactly = 1) { repository.currentUser }
    }

    @Test
    fun `GetCurrentUserUseCase returns null when nobody is signed in`() {
        every { repository.currentUser } returns null

        assertNull(GetCurrentUserUseCase(repository)())
    }

    @Test
    fun `SignInWithEmailUseCase passes credentials through`() =
        runTest {
            coEvery { repository.signInWithEmail(EMAIL, PASSWORD) } returns Result.success(USER)

            val result = SignInWithEmailUseCase(repository)(EMAIL, PASSWORD)

            assertEquals(USER, result.getOrNull())
            coVerify(exactly = 1) { repository.signInWithEmail(EMAIL, PASSWORD) }
        }

    @Test
    fun `SignInWithEmailUseCase propagates failure`() =
        runTest {
            coEvery { repository.signInWithEmail(any(), any()) } returns
                Result.failure(IllegalStateException(WRONG_PASSWORD))

            val result = SignInWithEmailUseCase(repository)(EMAIL, PASSWORD)

            assertTrue(result.isFailure)
            assertEquals(WRONG_PASSWORD, result.exceptionOrNull()?.message)
        }

    @Test
    fun `SignUpWithEmailUseCase passes credentials through`() =
        runTest {
            coEvery { repository.signUpWithEmail(EMAIL, PASSWORD) } returns Result.success(USER)

            val result = SignUpWithEmailUseCase(repository)(EMAIL, PASSWORD)

            assertEquals(USER, result.getOrNull())
            coVerify(exactly = 1) { repository.signUpWithEmail(EMAIL, PASSWORD) }
        }

    @Test
    fun `SignUpWithEmailUseCase propagates failure`() =
        runTest {
            coEvery { repository.signUpWithEmail(any(), any()) } returns
                Result.failure(IllegalStateException("email already in use"))

            assertTrue(SignUpWithEmailUseCase(repository)(EMAIL, PASSWORD).isFailure)
        }

    @Test
    fun `SignInWithGoogleUseCase passes the token through`() =
        runTest {
            coEvery { repository.signInWithGoogle(TOKEN) } returns Result.success(USER)

            val result = SignInWithGoogleUseCase(repository)(TOKEN)

            assertEquals(USER, result.getOrNull())
            coVerify(exactly = 1) { repository.signInWithGoogle(TOKEN) }
        }

    @Test
    fun `SignInWithGoogleUseCase propagates failure`() =
        runTest {
            coEvery { repository.signInWithGoogle(any()) } returns
                Result.failure(IllegalStateException("token expired"))

            assertTrue(SignInWithGoogleUseCase(repository)(TOKEN).isFailure)
        }

    @Test
    fun `SendPasswordResetEmailUseCase passes the address through`() =
        runTest {
            coEvery { repository.sendPasswordResetEmail(EMAIL) } returns Result.success(Unit)

            assertTrue(SendPasswordResetEmailUseCase(repository)(EMAIL).isSuccess)
            coVerify(exactly = 1) { repository.sendPasswordResetEmail(EMAIL) }
        }

    @Test
    fun `SendPasswordResetEmailUseCase propagates failure`() =
        runTest {
            coEvery { repository.sendPasswordResetEmail(any()) } returns
                Result.failure(IllegalStateException("network error"))

            assertTrue(SendPasswordResetEmailUseCase(repository)(EMAIL).isFailure)
        }

    private companion object {
        const val EMAIL = "user@example.com"
        const val PASSWORD = "secret1a"
        const val TOKEN = "google-id-token"
        const val WRONG_PASSWORD = "The password is invalid"

        val USER =
            AuthUser(
                uid = "uid",
                email = EMAIL,
                displayName = "User",
                photoUrl = null,
                hasPasswordProvider = true,
            )
    }
}
