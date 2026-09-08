package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerBestPrice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyBestPriceChangesUseCaseTest {
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = ApplyBestPriceChangesUseCase(repository)

    @Test
    fun `empty batch never reaches the database`() =
        runTest {
            // флаш тиков срабатывает по таймеру и часто застаёт пустую пачку —
            // писать в БД в этом случае нельзя, иначе Room зря инвалидирует страницы
            val result = useCase(emptyList())

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { repository.applyBestPriceUpdates(any()) }
        }

    @Test
    fun `non empty batch is passed through as is`() =
        runTest {
            val updates = listOf(bestPrice(symbolId = 1), bestPrice(symbolId = 2))
            coEvery { repository.applyBestPriceUpdates(updates) } returns Result.success(Unit)

            val result = useCase(updates)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.applyBestPriceUpdates(updates) }
        }

    @Test
    fun `repository failure is propagated`() =
        runTest {
            coEvery {
                repository.applyBestPriceUpdates(any())
            } returns Result.failure(IllegalStateException("db locked"))

            val result = useCase(listOf(bestPrice(symbolId = 1)))

            assertTrue(result.isFailure)
            assertEquals("db locked", result.exceptionOrNull()?.message)
        }

    private fun bestPrice(symbolId: Long) =
        TickerBestPrice(
            ticker = "btcusdt",
            symbolId = symbolId,
            bestAskProviderId = 18,
            bestAskPrice = 100.0,
            bestBidProviderId = 3,
            bestBidPrice = 99.0,
            spreadPercent = -1.0,
        )
}
