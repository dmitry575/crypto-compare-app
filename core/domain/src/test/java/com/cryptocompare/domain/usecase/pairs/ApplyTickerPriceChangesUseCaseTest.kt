package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.ticker.TickerPrice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyTickerPriceChangesUseCaseTest {
    private val repository: CryptoCompareRepository = mockk()
    private val useCase = ApplyTickerPriceChangesUseCase(repository)

    @Test
    fun `empty batch never reaches the database`() =
        runTest {
            // флаш тиков срабатывает по таймеру и часто застаёт пустую пачку —
            // писать в БД в этом случае нельзя, иначе Room зря инвалидирует страницы
            val result = useCase(emptyList())

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { repository.applyPriceUpdates(any()) }
        }

    @Test
    fun `non empty batch is passed through as is`() =
        runTest {
            val updates = listOf(price(symbolId = 1), price(symbolId = 2))
            coEvery { repository.applyPriceUpdates(updates) } returns Result.success(Unit)

            val result = useCase(updates)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.applyPriceUpdates(updates) }
        }

    @Test
    fun `repository failure is propagated`() =
        runTest {
            coEvery { repository.applyPriceUpdates(any()) } returns Result.failure(IllegalStateException("db locked"))

            val result = useCase(listOf(price(symbolId = 1)))

            assertTrue(result.isFailure)
            assertEquals("db locked", result.exceptionOrNull()?.message)
        }

    private fun price(symbolId: Int) =
        TickerPrice(
            ticker = "btcusdt",
            symbolId = symbolId,
            providerId = 1,
            priceSell = 100.0,
            priceBuy = 99.0,
        )
}
