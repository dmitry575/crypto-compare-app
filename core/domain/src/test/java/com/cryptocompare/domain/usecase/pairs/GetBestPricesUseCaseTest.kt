package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerBestPrice
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBestPricesUseCaseTest {
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = GetBestPricesUseCase(repository)

    @Test
    fun `incomplete pairs are dropped before they reach the screen`() =
        runTest {
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(listOf(best(143, askId = 18), best(12022, askId = null)))

            val result = useCase(TICKER).getOrThrow()

            assertEquals(listOf(143L), result.map { it.symbolId })
        }

    @Test
    fun `a symbol id keeps only its own best pair`() =
        runTest {
            coEvery { repository.getBestPricesByTicker(TICKER) } returns
                Result.success(listOf(best(143, askId = 18), best(14487, askId = 17)))

            val result = useCase(TICKER, symbolId = 14487).getOrThrow()

            assertEquals(listOf(14487L), result.map { it.symbolId })
        }

    @Test
    fun `a failed request stays a failure`() =
        runTest {
            coEvery { repository.getBestPricesByTicker(TICKER) } returns Result.failure(IllegalStateException("500"))

            assertTrue(useCase(TICKER).isFailure)
        }

    private fun best(
        symbolId: Long,
        askId: Int?,
    ) = TickerBestPrice(
        ticker = TICKER,
        symbolId = symbolId,
        bestAskProviderId = askId,
        bestAskPrice = 2511.42,
        bestBidProviderId = 7,
        bestBidPrice = 2513.39,
        spreadPercent = 0.0784,
    )

    private companion object {
        const val TICKER = "ethusdc"
    }
}
