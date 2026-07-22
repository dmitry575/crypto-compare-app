package com.cryptocompare.domain.usecase.pairs

import app.cash.turbine.test
import com.cryptocompare.domain.repository.FavouriteTickerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use case'ы избранного — тонкие обёртки над [FavouriteTickerRepository]. */
class FavouriteTickerUseCasesTest {
    private val repository: FavouriteTickerRepository = mockk()

    @Test
    fun `ObserveFavouriteTickersUseCase forwards every emission`() =
        runTest {
            every { repository.observeFavouriteTickers() } returns
                flowOf(setOf("btcusdt"), setOf("btcusdt", "ethusdt"))

            ObserveFavouriteTickersUseCase(repository)().test {
                assertEquals(setOf("btcusdt"), awaitItem())
                assertEquals(setOf("btcusdt", "ethusdt"), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `ToggleFavouriteTickerUseCase reports the new state`() =
        runTest {
            coEvery { repository.toggleFavouriteTicker(TICKER) } returns Result.success(true)

            val result = ToggleFavouriteTickerUseCase(repository)(TICKER)

            assertEquals(true, result.getOrNull())
            coVerify(exactly = 1) { repository.toggleFavouriteTicker(TICKER) }
        }

    @Test
    fun `ToggleFavouriteTickerUseCase propagates failure`() =
        runTest {
            coEvery { repository.toggleFavouriteTicker(any()) } returns
                Result.failure(IllegalStateException("User not authorized"))

            val result = ToggleFavouriteTickerUseCase(repository)(TICKER)

            assertTrue(result.isFailure)
            assertEquals("User not authorized", result.exceptionOrNull()?.message)
        }

    @Test
    fun `SyncFavouriteTickersUseCase delegates to the repository`() =
        runTest {
            coEvery { repository.syncFavouriteTickers() } returns Result.success(Unit)

            assertTrue(SyncFavouriteTickersUseCase(repository)().isSuccess)
            coVerify(exactly = 1) { repository.syncFavouriteTickers() }
        }

    @Test
    fun `SyncFavouriteTickersUseCase propagates failure`() =
        runTest {
            coEvery { repository.syncFavouriteTickers() } returns Result.failure(IllegalStateException("offline"))

            assertTrue(SyncFavouriteTickersUseCase(repository)().isFailure)
        }

    private companion object {
        const val TICKER = "btcusdt"
    }
}
