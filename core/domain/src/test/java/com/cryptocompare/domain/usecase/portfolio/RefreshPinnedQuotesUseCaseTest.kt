package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioQuote
import com.cryptocompare.model.symbol.Symbol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPinnedQuotesUseCaseTest {
    private val cryptoCompareRepository: CryptoCompareRepository = mockk()
    private val portfolioRepository: PortfolioRepository = mockk()
    private val useCase = RefreshPinnedQuotesUseCase(cryptoCompareRepository, portfolioRepository)

    @Test
    fun `the price is the bid of the position's own exchange`() =
        runTest {
            coEvery { cryptoCompareRepository.getSymbolsByTicker("btcusdt") } returns
                Result.success(
                    listOf(
                        row(symbolId = BTC, providerId = OKX, ask = 81_310.0, bid = 81_300.0),
                        row(symbolId = BTC, providerId = BYBIT, ask = 81_060.0, bid = 81_050.0, quotedAt = 42L),
                    ),
                )
            val saved = slot<List<PortfolioQuote>>()
            coEvery { portfolioRepository.savePinnedQuotes(capture(saved)) } returns Result.success(Unit)

            val result = useCase(listOf(position(BTC, "BTCUSDT", BYBIT)))

            // в разбивке имена от лица биржи: её bid — это priceBuy
            assertEquals(1, result.getOrThrow())
            assertEquals(listOf(PortfolioQuote(BTC, BYBIT, 81_050.0, 42L)), saved.captured)
        }

    @Test
    fun `another network of the same ticker is not the position's price`() =
        runTest {
            // USDC в Solana и USDC в Ethereum — разные активы с одним тикером
            coEvery { cryptoCompareRepository.getSymbolsByTicker("usdcusdt") } returns
                Result.success(listOf(row(symbolId = SOLANA_USDC, providerId = BYBIT, ask = 1.001, bid = 1.0)))

            val result = useCase(listOf(position(ETHEREUM_USDC, "USDCUSDT", BYBIT)))

            assertEquals(0, result.getOrThrow())
            coVerify(exactly = 0) { portfolioRepository.savePinnedQuotes(any()) }
        }

    @Test
    fun `one request per ticker, and a failed ticker does not hold the others back`() =
        runTest {
            coEvery { cryptoCompareRepository.getSymbolsByTicker("usdcusdt") } returns
                Result.failure(IllegalStateException("500"))
            coEvery { cryptoCompareRepository.getSymbolsByTicker("btcusdt") } returns
                Result.success(listOf(row(symbolId = BTC, providerId = BYBIT, ask = 81_060.0, bid = 81_050.0)))
            val saved = slot<List<PortfolioQuote>>()
            coEvery { portfolioRepository.savePinnedQuotes(capture(saved)) } returns Result.success(Unit)

            useCase(
                listOf(
                    position(BTC, "BTCUSDT", BYBIT),
                    position(ETHEREUM_USDC, "USDCUSDT", BYBIT),
                    position(SOLANA_USDC, "USDCUSDT", OKX),
                ),
            )

            assertEquals(listOf(BTC), saved.captured.map { it.symbolId })
            coVerify(exactly = 1) { cryptoCompareRepository.getSymbolsByTicker("usdcusdt") }
        }

    @Test
    fun `a zero bid is not saved as a price`() =
        runTest {
            coEvery { cryptoCompareRepository.getSymbolsByTicker("btcusdt") } returns
                Result.success(listOf(row(symbolId = BTC, providerId = BYBIT, ask = 81_060.0, bid = 0.0)))

            val result = useCase(listOf(position(BTC, "BTCUSDT", BYBIT)))

            assertEquals(0, result.getOrThrow())
            coVerify(exactly = 0) { portfolioRepository.savePinnedQuotes(any()) }
        }

    @Test
    fun `positions without an exchange are left to the catalog`() =
        runTest {
            val result = useCase(listOf(position(BTC, "BTCUSDT", providerId = null)))

            assertEquals(0, result.getOrThrow())
            coVerify(exactly = 0) { cryptoCompareRepository.getSymbolsByTicker(any()) }
        }

    private fun position(
        symbolId: Long,
        ticker: String,
        providerId: Int?,
    ) = PortfolioPosition(
        symbolId = symbolId,
        ticker = ticker,
        amount = 1.0,
        buyPrice = 1.0,
        updatedAtMillis = 1L,
        providerId = providerId,
    )

    private fun row(
        symbolId: Long,
        providerId: Int,
        ask: Double,
        bid: Double,
        quotedAt: Long? = null,
    ) = Symbol(
        id = symbolId,
        ticker = "btcusdt",
        symbol = "BTC/USDT",
        providerId = providerId,
        priceSell = ask,
        priceBuy = bid,
        quotedAtMillis = quotedAt,
    )

    private companion object {
        const val BTC = 1L
        const val ETHEREUM_USDC = 143L
        const val SOLANA_USDC = 144L
        const val BYBIT = 5
        const val OKX = 7
    }
}
