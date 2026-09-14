package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.ticker.TickerBestPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ApplyComparisonBestPricesUseCaseTest {
    private val useCase = ApplyComparisonBestPricesUseCase()

    @Test
    fun `an update of the shown symbol moves the summary`() {
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        val updated = useCase(comparison, listOf(best(symbolId = 143, askId = 6, bidId = 11, spread = 0.12)))

        assertEquals(6, updated.bestAskProviderId)
        assertEquals(11, updated.bestBidProviderId)
        assertEquals(0.12, updated.spreadPercent!!, 1e-9)
    }

    @Test
    fun `a narrower symbol does not take the summary over`() {
        // ethusdc 2026-09-14: символ 143 даёт +0.078%, а 14487 — −0.008% на одной
        // бирже. Раньше выжимку заменяло последнее событие, какое бы оно ни было
        val comparison =
            comparisonOf(
                best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784),
                best(symbolId = 14487, askId = 17, bidId = 17, spread = -0.008),
            )

        val updated = useCase(comparison, listOf(best(symbolId = 14487, askId = 17, bidId = 17, spread = -0.007)))

        assertEquals(18, updated.bestAskProviderId)
        assertEquals(0.0784, updated.spreadPercent!!, 1e-9)
    }

    @Test
    fun `another symbol wins once it becomes the widest`() {
        val comparison =
            comparisonOf(
                best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784),
                best(symbolId = 14487, askId = 17, bidId = 17, spread = -0.008),
            )

        val updated = useCase(comparison, listOf(best(symbolId = 14487, askId = 17, bidId = 17, spread = 0.2)))

        assertEquals(17, updated.bestAskProviderId)
        assertEquals(0.2, updated.spreadPercent!!, 1e-9)
    }

    @Test
    fun `the shown symbol narrowing hands the summary to the next widest`() {
        val comparison =
            comparisonOf(
                best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784),
                best(symbolId = 12022, askId = 2, bidId = 2, spread = -0.0004),
            )

        val updated = useCase(comparison, listOf(best(symbolId = 143, askId = 18, bidId = 7, spread = -0.3)))

        assertEquals(2, updated.bestAskProviderId)
        assertEquals(-0.0004, updated.spreadPercent!!, 1e-9)
    }

    @Test
    fun `a symbol first seen live joins the candidates`() {
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        val updated = useCase(comparison, listOf(best(symbolId = 99, askId = 3, bidId = 4, spread = 0.5)))

        assertEquals(2, updated.bestPrices.size)
        assertEquals(3, updated.bestAskProviderId)
    }

    @Test
    fun `an update without one side is skipped whole`() {
        // иначе отметка встала бы на новую биржу, а цена в выжимке осталась
        // бы от прежней пары
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        val updated =
            useCase(
                comparison,
                listOf(best(symbolId = 143, askId = 6, bidId = 11, spread = 0.5, askPrice = 0.0)),
            )

        assertSame(comparison, updated)
    }

    @Test
    fun `an update without an exchange is skipped whole`() {
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        val updated = useCase(comparison, listOf(best(symbolId = 143, askId = null, bidId = 11, spread = 0.5)))

        assertSame(comparison, updated)
    }

    @Test
    fun `an update with a non finite price is skipped whole`() {
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        val updated =
            useCase(
                comparison,
                listOf(best(symbolId = 143, askId = 6, bidId = 11, spread = 0.5, bidPrice = Double.NaN)),
            )

        assertSame(comparison, updated)
    }

    @Test
    fun `the backend answer replaces the single exchange fallback`() {
        // при одной бирже выжимку при загрузке заполняет она сама; как только
        // бэкенд прислал свою пару, решает он
        val fallback =
            PairComparison(
                ticker = TICKER,
                quotes = emptyList(),
                bestAskProviderId = 7,
                bestAskPrice = 0.03156,
                bestBidProviderId = 7,
                bestBidPrice = 0.03154,
                spreadPercent = -0.06,
                bestPrices = emptyList(),
            )

        val updated = useCase(fallback, listOf(best(symbolId = 1, askId = 7, bidId = 7, spread = -0.03)))

        assertEquals(-0.03, updated.spreadPercent!!, 1e-9)
    }

    @Test
    fun `nothing to apply returns the same comparison`() {
        val comparison = comparisonOf(best(symbolId = 143, askId = 18, bidId = 7, spread = 0.0784))

        assertSame(comparison, useCase(comparison, emptyList()))
    }

    private fun comparisonOf(vararg bestPrices: TickerBestPrice): PairComparison {
        val widest = bestPrices.maxBy { it.spreadPercent!! }

        return PairComparison(
            ticker = TICKER,
            quotes = emptyList(),
            bestAskProviderId = widest.bestAskProviderId,
            bestAskPrice = widest.bestAskPrice,
            bestBidProviderId = widest.bestBidProviderId,
            bestBidPrice = widest.bestBidPrice,
            spreadPercent = widest.spreadPercent,
            bestPrices = bestPrices.toList(),
        )
    }

    private fun best(
        symbolId: Long,
        askId: Int?,
        bidId: Int?,
        spread: Double,
        askPrice: Double = 2511.42,
        bidPrice: Double = 2513.39,
    ) = TickerBestPrice(
        ticker = TICKER,
        symbolId = symbolId,
        bestAskProviderId = askId,
        bestAskPrice = askPrice,
        bestBidProviderId = bidId,
        bestBidPrice = bidPrice,
        spreadPercent = spread,
    )

    private companion object {
        const val TICKER = "ethusdc"
    }
}
