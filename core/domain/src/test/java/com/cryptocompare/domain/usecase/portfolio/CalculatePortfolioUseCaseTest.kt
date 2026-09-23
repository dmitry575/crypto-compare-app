package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.symbol.SymbolSellQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatePortfolioUseCaseTest {
    private val calculatePortfolio = CalculatePortfolioUseCase()

    @Test
    fun `a position that grew shows profit in money and in percent`() {
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.5, buyPrice = 60_000.0)),
                mapOf(
                    BTC to quote(72_000.0),
                ),
            )

        val holding = portfolio.holdings.single()
        assertEquals(30_000.0, holding.invested, DELTA)
        assertEquals(36_000.0, holding.currentValue!!, DELTA)
        assertEquals(6_000.0, holding.profit!!, DELTA)
        assertEquals(20.0, holding.profitPercent!!, DELTA)
    }

    @Test
    fun `a position that fell shows a negative profit`() {
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.5, buyPrice = 60_000.0)),
                mapOf(
                    BTC to quote(45_000.0),
                ),
            )

        val holding = portfolio.holdings.single()
        assertEquals(-7_500.0, holding.profit!!, DELTA)
        assertEquals(-25.0, holding.profitPercent!!, DELTA)
    }

    @Test
    fun `decimal money arithmetic leaves no float tails`() {
        // Double на этих же числах даёт 0.030000000000000002 и прибыль
        // 0.010000000000000002 — в деньгах это мусор, который вылезает в UI
        val portfolio = calculatePortfolio(listOf(position(amount = 0.1, buyPrice = 0.2)), mapOf(BTC to quote(0.3)))

        val holding = portfolio.holdings.single()
        assertEquals(0.02, holding.invested, EXACT)
        assertEquals(0.03, holding.currentValue!!, EXACT)
        assertEquals(0.01, holding.profit!!, EXACT)
        assertEquals(50.0, holding.profitPercent!!, EXACT)
    }

    @Test
    fun `a position at its buy price is exactly flat`() {
        val portfolio = calculatePortfolio(listOf(position(amount = 0.07, buyPrice = 8.1)), mapOf(BTC to quote(8.1)))

        val holding = portfolio.holdings.single()
        assertEquals(0.0, holding.profit!!, EXACT)
        assertEquals(0.0, holding.profitPercent!!, EXACT)
    }

    @Test
    fun `zero amount costs nothing and is worth nothing`() {
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.0, buyPrice = 60_000.0)),
                mapOf(
                    BTC to quote(72_000.0),
                ),
            )

        val holding = portfolio.holdings.single()
        assertEquals(0.0, holding.invested, EXACT)
        assertEquals(0.0, holding.currentValue!!, EXACT)
        assertEquals(0.0, holding.profit!!, EXACT)
        // делить не на что: процента у нулевого вложения нет
        assertNull(holding.profitPercent)
    }

    @Test
    fun `zero investment gives profit in money but not in percent`() {
        // монета досталась даром — эйрдроп, форк; прибыль есть, а доли от нуля нет
        val portfolio = calculatePortfolio(listOf(position(amount = 10.0, buyPrice = 0.0)), mapOf(BTC to quote(3.0)))

        val holding = portfolio.holdings.single()
        assertEquals(0.0, holding.invested, EXACT)
        assertEquals(30.0, holding.currentValue!!, EXACT)
        assertEquals(30.0, holding.profit!!, EXACT)
        assertNull(holding.profitPercent)
    }

    @Test
    fun `a position without a price keeps what is known and leaves the rest empty`() {
        val portfolio = calculatePortfolio(listOf(position(amount = 0.5, buyPrice = 60_000.0)), emptyMap())

        val holding = portfolio.holdings.single()
        assertEquals(30_000.0, holding.invested, DELTA)
        assertNull(holding.currentPrice)
        assertNull(holding.currentValue)
        assertNull(holding.profit)
        assertNull(holding.profitPercent)
        // считать нечего: итога нет
        assertNull(portfolio.summary)
    }

    @Test
    fun `a zero or broken price counts as no price at all`() {
        // ноль у биржи означает «стороны стакана нет», а не «актив обесценился»
        val prices = mapOf(BTC to quote(0.0), ETH to quote(Double.NaN))
        val positions =
            listOf(
                position(amount = 1.0, buyPrice = 10.0),
                position(symbolId = ETH, ticker = "ETHUSDT", amount = 1.0, buyPrice = 10.0),
            )

        val portfolio = calculatePortfolio(positions, prices)

        assertNull(portfolio.holdings[0].currentValue)
        assertNull(portfolio.holdings[1].currentValue)
        assertNull(portfolio.summary)
    }

    @Test
    fun `the total sums up several positions`() {
        val positions =
            listOf(
                position(amount = 0.5, buyPrice = 60_000.0),
                position(symbolId = ETH, ticker = "ETHUSDT", amount = 4.0, buyPrice = 2_000.0),
            )

        val portfolio = calculatePortfolio(positions, mapOf(BTC to quote(72_000.0), ETH to quote(1_500.0)))

        val summary = portfolio.summary!!
        assertEquals(38_000.0, summary.invested, DELTA)
        assertEquals(42_000.0, summary.currentValue, DELTA)
        assertEquals(4_000.0, summary.profit, DELTA)
        assertEquals(10.526315789473685, summary.profitPercent!!, DELTA)
        assertEquals(0, summary.positionsWithoutPrice)
    }

    @Test
    fun `the total covers only priced positions and says how many it skipped`() {
        // иначе вложенное по всему портфелю складывалось бы со стоимостью его
        // части, и выпавший из каталога символ выглядел бы как убыток
        val positions =
            listOf(
                position(amount = 0.5, buyPrice = 60_000.0),
                position(symbolId = ETH, ticker = "ETHUSDT", amount = 4.0, buyPrice = 2_000.0),
            )

        val portfolio = calculatePortfolio(positions, mapOf(BTC to quote(72_000.0)))

        val summary = portfolio.summary!!
        assertEquals(30_000.0, summary.invested, DELTA)
        assertEquals(36_000.0, summary.currentValue, DELTA)
        assertEquals(6_000.0, summary.profit, DELTA)
        assertEquals(20.0, summary.profitPercent!!, DELTA)
        assertEquals(1, summary.positionsWithoutPrice)
    }

    @Test
    fun `large positions stay exact`() {
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 1_000_000.0, buyPrice = 1_000_000.0)),
                mapOf(BTC to quote(2_000_000.0)),
            )

        val holding = portfolio.holdings.single()
        assertEquals(1e12, holding.invested, EXACT)
        assertEquals(2e12, holding.currentValue!!, EXACT)
        assertEquals(1e12, holding.profit!!, EXACT)
        assertEquals(100.0, holding.profitPercent!!, EXACT)
    }

    @Test
    fun `tiny prices do not collapse to zero`() {
        // округление до фиксированного знака съедало бы позиции в дешёвых парах
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.001, buyPrice = 0.000000001)),
                mapOf(BTC to quote(0.000000002)),
            )

        val holding = portfolio.holdings.single()
        assertEquals(1e-12, holding.invested, EXACT)
        assertEquals(2e-12, holding.currentValue!!, EXACT)
        assertEquals(100.0, holding.profitPercent!!, EXACT)
    }

    @Test
    fun `an empty portfolio has no total`() {
        val portfolio = calculatePortfolio(emptyList(), emptyMap())

        assertEquals(emptyList<Any>(), portfolio.holdings)
        assertNull(portfolio.summary)
    }

    @Test
    fun `a priced holding names the exchange its price came from`() {
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.5, buyPrice = 60_000.0)),
                mapOf(BTC to quote(72_000.0, exchange = "bitget")),
            )

        assertEquals("bitget", portfolio.holdings.single().priceExchange)
    }

    @Test
    fun `a holding without a usable price names no exchange`() {
        // «по цене bitget» рядом с прочерком читалось бы как «bitget отдаёт ноль»
        val portfolio =
            calculatePortfolio(
                listOf(position(amount = 0.5, buyPrice = 60_000.0)),
                mapOf(BTC to quote(0.0, exchange = "bitget")),
            )

        assertNull(portfolio.holdings.single().priceExchange)
    }

    private fun quote(
        price: Double,
        exchange: String? = "binance",
    ) = SymbolSellQuote(price = price, providerId = 1, exchangeName = exchange)

    private fun position(
        symbolId: Long = BTC,
        ticker: String = "BTCUSDT",
        amount: Double,
        buyPrice: Double,
    ) = PortfolioPosition(
        symbolId = symbolId,
        ticker = ticker,
        amount = amount,
        buyPrice = buyPrice,
        updatedAtMillis = 1_700_000_000_000L,
    )

    private companion object {
        const val BTC = 1L
        const val ETH = 2L

        const val DELTA = 1e-9

        /** Точное сравнение: в деньгах хвост от `Double` — это и есть то, что чинится. */
        const val EXACT = 0.0
    }
}
