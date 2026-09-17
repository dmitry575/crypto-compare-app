package com.cryptocompare.domain.usecase.pairs

import app.cash.turbine.test
import com.cryptocompare.domain.repository.FavouriteSymbolRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use case'ы избранного — тонкие обёртки над [FavouriteSymbolRepository]. */
class FavouriteSymbolUseCasesTest {
    private val repository: FavouriteSymbolRepository = mockk()

    @Test
    fun `ObserveFavouriteSymbolsUseCase forwards every emission`() =
        runTest {
            every { repository.observeFavouriteSymbolIds() } returns flowOf(setOf(1L), setOf(1L, 14805L))

            ObserveFavouriteSymbolsUseCase(repository)().test {
                assertEquals(setOf(1L), awaitItem())
                assertEquals(setOf(1L, 14805L), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `ToggleFavouriteSymbolUseCase reports the new state`() =
        runTest {
            coEvery { repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER) } returns Result.success(true)

            val result = ToggleFavouriteSymbolUseCase(repository)(SYMBOL_ID, TICKER)

            assertEquals(true, result.getOrNull())
            coVerify(exactly = 1) { repository.toggleFavouriteSymbol(SYMBOL_ID, TICKER) }
        }

    @Test
    fun `ToggleFavouriteSymbolUseCase propagates failure`() =
        runTest {
            coEvery { repository.toggleFavouriteSymbol(any(), any()) } returns
                Result.failure(IllegalStateException("User not authorized"))

            val result = ToggleFavouriteSymbolUseCase(repository)(SYMBOL_ID, TICKER)

            assertTrue(result.isFailure)
            assertEquals("User not authorized", result.exceptionOrNull()?.message)
        }

    @Test
    fun `SyncFavouriteSymbolsUseCase delegates to the repository`() =
        runTest {
            coEvery { repository.syncFavouriteSymbols() } returns Result.success(Unit)

            assertTrue(SyncFavouriteSymbolsUseCase(repository)().isSuccess)
            coVerify(exactly = 1) { repository.syncFavouriteSymbols() }
        }

    @Test
    fun `SyncFavouriteSymbolsUseCase propagates failure`() =
        runTest {
            coEvery { repository.syncFavouriteSymbols() } returns Result.failure(IllegalStateException("offline"))

            assertTrue(SyncFavouriteSymbolsUseCase(repository)().isFailure)
        }

    private companion object {
        const val SYMBOL_ID = 1L
        const val TICKER = "btcusdt"
    }
}
