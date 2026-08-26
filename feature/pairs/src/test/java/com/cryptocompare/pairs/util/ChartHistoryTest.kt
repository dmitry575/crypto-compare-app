package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartHistoryTest {
    private fun candle(timeMillis: Long) =
        Candle(timeMillis = timeMillis, open = 1.0, high = 2.0, low = 0.5, close = 1.5)

    private fun candles(
        count: Int,
        startTime: Long,
    ) = List(count) { candle(startTime + it * STEP_MS) }

    @Test
    fun `an older page is prepended and the order is kept`() {
        val history = ChartHistory.initial(candles(count = 3, startTime = 300L))

        val grown = history.withOlderPage(candles(count = 3, startTime = 0L))

        assertEquals(6, grown.candles.size)
        assertEquals(0L, grown.candles.first().timeMillis)
        assertEquals(500L, grown.candles.last().timeMillis)
        assertTrue(grown.canLoadOlder)
    }

    @Test
    fun `an empty page ends the history`() {
        val history = ChartHistory.initial(candles(count = 3, startTime = 0L))

        val ended = history.withOlderPage(emptyList())

        assertFalse(ended.canLoadOlder)
        assertEquals(3, ended.candles.size)
    }

    @Test
    fun `a page without new candles ends the history`() {
        // биржа отдала ту же страницу ещё раз — глубже у неё ничего нет
        val page = candles(count = 3, startTime = 0L)
        val history = ChartHistory.initial(page)

        assertFalse(history.withOlderPage(page).canLoadOlder)
    }

    @Test
    fun `the next offset counts server candles only`() {
        val history = ChartHistory.initial(candles(count = 100, startTime = 0L))
        assertEquals(100, history.oldestSkip)

        // живой тик открыл новый бар: он не серверный и в offset не попадает
        val live = history.withLiveCandles(history.candles + candle(100 * STEP_MS))
        assertEquals(1, live.liveCount)
        assertEquals(100, live.oldestSkip)
        assertEquals(1, live.newestAbs)

        // и не сдвигает индекс самой старой свечи
        assertEquals(history.oldestAbs, live.oldestAbs)
    }

    @Test
    fun `a tick that only moves the last bar does not add a live candle`() {
        val history = ChartHistory.initial(candles(count = 5, startTime = 0L))

        val moved = history.withLiveCandles(history.candles.dropLast(1) + candle(4 * STEP_MS))

        assertEquals(0, moved.liveCount)
        assertEquals(5, moved.serverCount)
    }

    @Test
    fun `paging stops at the in-memory limit`() {
        val full = ChartHistory.initial(candles(count = PairsConstants.Chart.MAX_CANDLES, startTime = 0L))

        assertFalse("история в памяти не ограничена", full.canLoadOlder)
    }

    @Test
    fun `an exchange without history is not asked again`() {
        assertFalse(ChartHistory.initial(emptyList()).canLoadOlder)
    }

    private companion object {
        const val STEP_MS = 100L
    }
}
