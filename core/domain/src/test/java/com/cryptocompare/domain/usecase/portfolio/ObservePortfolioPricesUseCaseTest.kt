package com.cryptocompare.domain.usecase.portfolio

import app.cash.turbine.test
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.symbol.SymbolSellQuote
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservePortfolioPricesUseCaseTest {
    private val cryptoCompareRepository: CryptoCompareRepository = mockk()
    private val portfolioRepository: PortfolioRepository = mockk()
    private val useCase = ObservePortfolioPricesUseCase(cryptoCompareRepository, portfolioRepository)

    @Test
    fun `a position with an exchange takes its price, one without takes the catalog`() =
        runTest {
            every { cryptoCompareRepository.observeSellQuotes(setOf(ETH)) } returns
                flowOf(mapOf(ETH to BEST_ETH))
            every { portfolioRepository.observePinnedQuotes() } returns flowOf(mapOf(BTC to BYBIT_BTC))

            useCase(listOf(position(BTC, providerId = 5), position(ETH, providerId = null))).test {
                assertEquals(mapOf(BTC to BYBIT_BTC, ETH to BEST_ETH), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `the best bid does not stand in for a position with an exchange`() =
        runTest {
            // цены биржи ещё нет — лучше прочерк, чем цена площадки, где монеты нет
            every { cryptoCompareRepository.observeSellQuotes(emptySet()) } returns flowOf(emptyMap())
            every { portfolioRepository.observePinnedQuotes() } returns flowOf(emptyMap())

            useCase(listOf(position(BTC, providerId = 5))).test {
                assertEquals(emptyMap<Long, SymbolSellQuote>(), awaitItem())
                awaitComplete()
            }
            verify(exactly = 0) { cryptoCompareRepository.observeSellQuotes(setOf(BTC)) }
        }

    @Test
    fun `a portfolio without exchanges does not read the stored quotes at all`() =
        runTest {
            every { cryptoCompareRepository.observeSellQuotes(setOf(BTC)) } returns flowOf(mapOf(BTC to BEST_ETH))

            useCase(listOf(position(BTC, providerId = null))).test {
                assertEquals(mapOf(BTC to BEST_ETH), awaitItem())
                awaitComplete()
            }
            verify(exactly = 0) { portfolioRepository.observePinnedQuotes() }
        }

    private fun position(
        symbolId: Long,
        providerId: Int?,
    ) = PortfolioPosition(
        symbolId = symbolId,
        ticker = "BTCUSDT",
        amount = 1.0,
        buyPrice = 1.0,
        updatedAtMillis = 1L,
        providerId = providerId,
    )

    private companion object {
        const val BTC = 1L
        const val ETH = 2L

        val BYBIT_BTC = SymbolSellQuote(price = 81_050.0, providerId = 5, exchangeName = "bybit")
        val BEST_ETH = SymbolSellQuote(price = 4_000.0, providerId = 2, exchangeName = "bitget")
    }
}
