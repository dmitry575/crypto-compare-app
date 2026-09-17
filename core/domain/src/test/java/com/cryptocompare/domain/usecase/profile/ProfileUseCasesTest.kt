package com.cryptocompare.domain.usecase.profile

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
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
    private val favouriteSymbolRepository: FavouriteSymbolRepository = mockk(relaxed = true)

    @Test
    fun `SignOutUseCase delegates to the repository`() =
        runTest {
            SignOutUseCase(repository)()
            coVerify(exactly = 1) { repository.signOut() }
        }

    @Test
    fun `DeleteAccountUseCase deletes favourites before the account`() =
        runTest {
            coEvery { favouriteSymbolRepository.deleteAllFavourites() } returns Result.success(Unit)
            coEvery { repository.deleteAccount() } returns Result.success(Unit)

            val result = DeleteAccountUseCase(repository, favouriteSymbolRepository)()

            assertTrue(result.isSuccess)
            coVerifyOrder {
                favouriteSymbolRepository.deleteAllFavourites()
                repository.deleteAccount()
            }
        }

    @Test
    fun `DeleteAccountUseCase does not delete the account when clearing favourites fails`() =
        runTest {
            coEvery { favouriteSymbolRepository.deleteAllFavourites() } returns
                Result.failure(IllegalStateException(FAVOURITES_ERROR))

            val result = DeleteAccountUseCase(repository, favouriteSymbolRepository)()

            assertTrue(result.isFailure)
            assertEquals(FAVOURITES_ERROR, result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { repository.deleteAccount() }
        }

    @Test
    fun `DeleteAccountUseCase propagates the recent login requirement`() =
        runTest {
            coEvery { favouriteSymbolRepository.deleteAllFavourites() } returns Result.success(Unit)
            coEvery { repository.deleteAccount() } returns Result.failure(IllegalStateException(RECENT_LOGIN))

            val result = DeleteAccountUseCase(repository, favouriteSymbolRepository)()

            assertTrue(result.isFailure)
            assertEquals(RECENT_LOGIN, result.exceptionOrNull()?.message)
        }

    private companion object {
        const val RECENT_LOGIN = "This operation requires recent authentication"
        const val FAVOURITES_ERROR = "Failed to clear favourites"
    }
}
