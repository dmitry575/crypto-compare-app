package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use case'ы профиля — тонкие обёртки над [AuthRepository]. */
class ProfileUseCasesTest {
    private val repository: AuthRepository = mockk(relaxed = true)

    @Test
    fun `SignOutUseCase delegates to the repository`() =
        runTest {
            SignOutUseCase(repository)()

            coVerify(exactly = 1) { repository.signOut() }
        }

    @Test
    fun `DeleteAccountUseCase returns success`() =
        runTest {
            coEvery { repository.deleteAccount() } returns Result.success(Unit)

            assertTrue(DeleteAccountUseCase(repository)().isSuccess)
            coVerify(exactly = 1) { repository.deleteAccount() }
        }

    @Test
    fun `DeleteAccountUseCase propagates the recent login requirement`() =
        runTest {
            coEvery { repository.deleteAccount() } returns Result.failure(IllegalStateException(RECENT_LOGIN))

            val result = DeleteAccountUseCase(repository)()

            assertTrue(result.isFailure)
            assertEquals(RECENT_LOGIN, result.exceptionOrNull()?.message)
        }

    @Test
    fun `ChangePasswordUseCase passes both passwords in the right order`() =
        runTest {
            coEvery { repository.changePassword(CURRENT, NEW) } returns Result.success(Unit)

            assertTrue(ChangePasswordUseCase(repository)(CURRENT, NEW).isSuccess)
            coVerify(exactly = 1) { repository.changePassword(CURRENT, NEW) }
        }

    @Test
    fun `ChangePasswordUseCase propagates a failed reauthentication`() =
        runTest {
            coEvery { repository.changePassword(any(), any()) } returns
                Result.failure(IllegalStateException(WRONG_PASSWORD))

            val result = ChangePasswordUseCase(repository)(CURRENT, NEW)

            assertTrue(result.isFailure)
            assertEquals(WRONG_PASSWORD, result.exceptionOrNull()?.message)
        }

    private companion object {
        const val CURRENT = "current1a"
        const val NEW = "secret1b"
        const val RECENT_LOGIN = "This operation requires recent authentication"
        const val WRONG_PASSWORD = "The password is invalid"
    }
}
