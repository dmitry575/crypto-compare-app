package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLastCandleTest {
    private val timeframe = ChartTimeframe.H1

    private fun candle(
        timeMillis: Long,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
    ) = Candle(
        timeMillis = timeMillis,
        open = open,
        high = high,
        low = low,
        close = close,
    )

    /** Часовые бары открываются на кратных 3_600_000 отметках. */
    private val history =
        listOf(
            candle(7_200_000, 100.0, 105.0, 95.0, 102.0),
            candle(10_800_000, 102.0, 106.0, 101.0, 104.0),
        )

    @Test
    fun `tick inside current bucket updates the last candle`() {
        // 11_000_000 попадает в бар, открытый в 10_800_000
        val updated = updateLastCandle(history, price = 109.5, timeframe = timeframe, nowMillis = 11_000_000)

        assertEquals(history.size, updated.size)
        assertEquals(history.first(), updated.first())

        val liveBar = updated.last()
        assertEquals(10_800_000, liveBar.timeMillis)
        assertEquals(102.0, liveBar.open, 0.0)
        assertEquals(109.5, liveBar.high, 0.0)
        assertEquals(101.0, liveBar.low, 0.0)
        assertEquals(109.5, liveBar.close, 0.0)
    }

    @Test
    fun `tick after the last bar opens a new candle at the price`() {
        // 14_500_000 относится к бару, открытому в 14_400_000, — позже всей истории
        val updated = updateLastCandle(history, price = 110.0, timeframe = timeframe, nowMillis = 14_500_000)

        assertEquals(history.size + 1, updated.size)
        assertEquals(history, updated.dropLast(1))

        val newBar = updated.last()
        assertEquals(14_400_000, newBar.timeMillis)
        assertEquals(110.0, newBar.open, 0.0)
        assertEquals(110.0, newBar.high, 0.0)
        assertEquals(110.0, newBar.low, 0.0)
        assertEquals(110.0, newBar.close, 0.0)
    }

    @Test
    fun `late tick still moves the last bar instead of rewriting history`() {
        // история с API запаздывает: тик по времени старше последней свечи,
        // но новую ретроспективную свечу из него делать нельзя
        val updated = updateLastCandle(history, price = 99.0, timeframe = timeframe, nowMillis = 9_000_000)

        assertEquals(history.size, updated.size)
        assertEquals(10_800_000, updated.last().timeMillis)
        assertEquals(99.0, updated.last().close, 0.0)
        assertEquals(99.0, updated.last().low, 0.0)
    }

    @Test
    fun `invalid price leaves candles untouched`() {
        listOf(Double.NaN, 0.0, -1.0).forEach { price ->
            assertEquals(
                history,
                updateLastCandle(history, price = price, timeframe = timeframe, nowMillis = 11_000_000),
            )
        }
    }

    @Test
    fun `empty history stays empty`() {
        assertEquals(
            emptyList<Candle>(),
            updateLastCandle(emptyList(), price = 100.0, timeframe = timeframe, nowMillis = 11_000_000),
        )
    }

    @Test
    fun `live bar keeps candle invariants`() {
        val updated = updateLastCandle(history, price = 103.3, timeframe = timeframe, nowMillis = 11_000_000)
        val bar = updated.last()

        assertTrue(bar.low <= minOf(bar.open, bar.close))
        assertTrue(maxOf(bar.open, bar.close) <= bar.high)
    }
}
