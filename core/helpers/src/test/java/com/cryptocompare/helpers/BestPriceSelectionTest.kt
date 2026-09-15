package com.cryptocompare.helpers

import com.cryptocompare.model.ticker.TickerBestPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BestPriceSelectionTest {
    @Test
    fun `a pair with both sides and both exchanges is complete`() {
        assertTrue(best(symbolId = 1, spread = 0.1).isComplete())
    }

    @Test
    fun `a pair without an exchange or with a broken price is not complete`() {
        assertFalse(best(symbolId = 1, spread = 0.1, askId = null).isComplete())
        assertFalse(best(symbolId = 1, spread = 0.1, askPrice = 0.0).isComplete())
        assertFalse(best(symbolId = 1, spread = 0.1, bidPrice = Double.NaN).isComplete())
    }

    @Test
    fun `the widest pair wins, whatever its sign`() {
        // ethusdc 2026-09-14: три символа, и каталог берёт MAX(spreadPercent)
        val pairs = listOf(best(143, 0.0784), best(12022, -0.0004), best(14487, -0.008))

        assertEquals(143L, pairs.widest()!!.symbolId)
    }

    @Test
    fun `a pair without a spread loses to any pair with one`() {
        val pairs = listOf(best(1, spread = null), best(2, spread = -3.0))

        assertEquals(2L, pairs.widest()!!.symbolId)
    }

    @Test
    fun `no pairs means no widest`() {
        assertNull(emptyList<TickerBestPrice>().widest())
    }

    @Test
    fun `an update replaces its own symbol and keeps the others`() {
        val current = listOf(best(143, 0.0784), best(14487, -0.008))

        val updated = current.withUpdates(listOf(best(14487, -0.007)))

        assertEquals(2, updated.size)
        assertEquals(-0.007, updated.single { it.symbolId == 14487L }.spreadPercent!!, 0.0)
        assertEquals(0.0784, updated.single { it.symbolId == 143L }.spreadPercent!!, 0.0)
    }

    @Test
    fun `a symbol seen for the first time is added`() {
        val updated = listOf(best(143, 0.0784)).withUpdates(listOf(best(99, 0.5)))

        assertEquals(setOf(143L, 99L), updated.map { it.symbolId }.toSet())
    }

    @Test
    fun `incomplete updates leave the very same list`() {
        val current = listOf(best(143, 0.0784))

        assertSame(current, current.withUpdates(listOf(best(143, 0.5, askPrice = 0.0))))
    }

    private fun best(
        symbolId: Long,
        spread: Double?,
        askId: Int? = 18,
        askPrice: Double = 2511.42,
        bidPrice: Double = 2513.39,
    ) = TickerBestPrice(
        ticker = "ethusdc",
        symbolId = symbolId,
        bestAskProviderId = askId,
        bestAskPrice = askPrice,
        bestBidProviderId = 7,
        bestBidPrice = bidPrice,
        spreadPercent = spread,
    )
}
