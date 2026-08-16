package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUseCasesTest {
    private val repository: AuthRepository = mockk(relaxed = true)
    private val favouriteTickerRepository: FavouriteTickerRepository = mockk(relaxed = true)

    @Test
    fun `SignOutUseCase delegates to the repository`() =
        runTest {
            SignOutUseCase(repository)()
            coVerify(exactly = 1) { repository.signOut() }
        }

    @Test
    fun `DeleteAccountUseCase deletes favourites before the account`() =
        runTest {
            coEvery { favouriteTickerRepository.deleteAllFavorites() } returns Result.success(Unit)
            coEvery { repository.deleteAccount() } returns Result.success(Unit)

            val result = DeleteAccountUseCase(repository, favouriteTickerRepository)()

            assertTrue(result.isSuccess)
            coVerifyOrder {
                favouriteTickerRepository.deleteAllFavorites()
                repository.deleteAccount()
            }
        }

    @Test
    fun `DeleteAccountUseCase does not delete the account when clearing favourites fails`() =
        runTest {
            coEvery { favouriteTickerRepository.deleteAllFavorites() } returns
                Result.failure(IllegalStateException(FAVOURITES_ERROR))

            val result = DeleteAccountUseCase(repository, favouriteTickerRepository)()

            assertTrue(result.isFailure)
            assertEquals(FAVOURITES_ERROR, result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { repository.deleteAccount() }
        }

    @Test
    fun `DeleteAccountUseCase propagates the recent login requirement`() =
        runTest {
            coEvery { favouriteTickerRepository.deleteAllFavorites() } returns Result.success(Unit)
            coEvery { repository.deleteAccount() } returns Result.failure(IllegalStateException(RECENT_LOGIN))

            val result = DeleteAccountUseCase(repository, favouriteTickerRepository)()

            assertTrue(result.isFailure)
            assertEquals(RECENT_LOGIN, result.exceptionOrNull()?.message)
        }

    private companion object {
        const val RECENT_LOGIN = "This operation requires recent authentication"
        const val FAVOURITES_ERROR = "Failed to clear favourites"
    }
}
