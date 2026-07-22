package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use case'ы каталога и истории — тонкие обёртки над [CryptoCompareRepository]. */
class CatalogUseCasesTest {
    private val repository: CryptoCompareRepository = mockk()

    @Test
    fun `GetTickerHistoryUseCase passes ticker and timeframe through`() =
        runTest {
            val candles = listOf(candle(1_751_328_000_000L), candle(1_751_331_600_000L))
            coEvery { repository.getCandles(TICKER, ChartTimeframe.H4) } returns Result.success(candles)

            val result = GetTickerHistoryUseCase(repository)(TICKER, ChartTimeframe.H4)

            assertEquals(candles, result.getOrNull())
            coVerify(exactly = 1) { repository.getCandles(TICKER, ChartTimeframe.H4) }
        }

    @Test
    fun `GetTickerHistoryUseCase asks for each timeframe separately`() =
        runTest {
            coEvery { repository.getCandles(any(), any()) } returns Result.success(emptyList())
            val useCase = GetTickerHistoryUseCase(repository)

            ChartTimeframe.entries.forEach { timeframe -> useCase(TICKER, timeframe) }

            ChartTimeframe.entries.forEach { timeframe ->
                coVerify(exactly = 1) { repository.getCandles(TICKER, timeframe) }
            }
        }

    @Test
    fun `GetTickerHistoryUseCase propagates a missing api key or rate limit`() =
        runTest {
            coEvery { repository.getCandles(any(), any()) } returns
                Result.failure(IllegalStateException(RATE_LIMIT))

            val result = GetTickerHistoryUseCase(repository)(TICKER, ChartTimeframe.DEFAULT)

            assertTrue(result.isFailure)
            assertEquals(RATE_LIMIT, result.exceptionOrNull()?.message)
        }

    @Test
    fun `RefreshCatalogUseCase delegates to the repository`() =
        runTest {
            coEvery { repository.refreshCatalog() } returns Result.success(Unit)

            assertTrue(RefreshCatalogUseCase(repository)().isSuccess)
            coVerify(exactly = 1) { repository.refreshCatalog() }
        }

    @Test
    fun `RefreshCatalogUseCase propagates failure so the worker can retry`() =
        runTest {
            coEvery { repository.refreshCatalog() } returns Result.failure(IllegalStateException("HTTP 404"))

            val result = RefreshCatalogUseCase(repository)()

            assertTrue(result.isFailure)
            assertEquals("HTTP 404", result.exceptionOrNull()?.message)
        }

    private fun candle(timeMillis: Long) =
        Candle(timeMillis = timeMillis, open = 10.0, high = 12.0, low = 9.0, close = 11.0)

    private companion object {
        const val TICKER = "btcusdt"
        const val RATE_LIMIT = "You are over your rate limit please upgrade your account"
    }
}
