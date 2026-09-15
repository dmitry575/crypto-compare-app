package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerBestPrice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshBestPricesUseCaseTest {
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = RefreshBestPricesUseCase(repository)

    @Test
    fun `best prices of every ticker are written in one batch`() =
        runTest {
            coEvery { repository.getBestPricesByTicker("btcusdt") } returns Result.success(listOf(best(1, "btcusdt")))
            coEvery { repository.getBestPricesByTicker("ethusdc") } returns
                Result.success(listOf(best(143, "ethusdc"), best(14487, "ethusdc")))
            val written = slot<List<TickerBestPrice>>()
            coEvery { repository.applyBestPriceUpdates(capture(written)) } returns Result.success(Unit)

            val result = useCase(setOf("btcusdt", "ethusdc"))

            assertTrue(result.isSuccess)
            // у тикера бывает несколько символов — пишутся все его строки
            assertEquals(setOf(1L, 143L, 14487L), written.captured.map { it.symbolId }.toSet())
            coVerify(exactly = 1) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `a failed ticker does not hold the others back`() =
        runTest {
            coEvery { repository.getBestPricesByTicker("btcusdt") } returns Result.failure(IllegalStateException("500"))
            coEvery { repository.getBestPricesByTicker("ethusdc") } returns Result.success(listOf(best(143, "ethusdc")))
            val written = slot<List<TickerBestPrice>>()
            coEvery { repository.applyBestPriceUpdates(capture(written)) } returns Result.success(Unit)

            useCase(setOf("btcusdt", "ethusdc"))

            assertEquals(listOf(143L), written.captured.map { it.symbolId })
        }

    @Test
    fun `nothing fetched means nothing written`() =
        runTest {
            coEvery { repository.getBestPricesByTicker(any()) } returns Result.failure(IllegalStateException("offline"))

            val result = useCase(setOf("btcusdt"))

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `no tickers means no requests`() =
        runTest {
            useCase(emptySet())

            coVerify(exactly = 0) { repository.getBestPricesByTicker(any()) }
        }

    private fun best(
        symbolId: Long,
        ticker: String,
    ) = TickerBestPrice(
        ticker = ticker,
        symbolId = symbolId,
        bestAskProviderId = 1,
        bestAskPrice = 100.0,
        bestBidProviderId = 2,
        bestBidPrice = 99.9,
        spreadPercent = -0.1,
    )
}
