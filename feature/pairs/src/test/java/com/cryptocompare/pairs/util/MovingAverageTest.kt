package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovingAverageTest {
    @Test
    fun `history shorter than the period gives no values at all`() {
        val series = candles(List(19) { 10.0 }).movingAverage(ChartIndicator.SMA_20)

        assertEquals(19, series.size)
        assertTrue(series.all { it == null })
    }

    @Test
    fun `the first values are empty until the period fills up`() {
        val series = candles(List(20) { 10.0 }).movingAverage(ChartIndicator.SMA_20)

        // тянуть линию от края значило бы показать тренд, которого нет
        assertTrue(series.take(19).all { it == null })
        assertEquals(10.0, series[19]!!, DELTA)
    }

    @Test
    fun `simple average moves by the candle that left the window`() {
        val series = candles(List(20) { 10.0 } + 20.0).movingAverage(ChartIndicator.SMA_20)

        // окно сдвинулось: ушла десятка, пришла двадцатка
        assertEquals(10.5, series[20]!!, DELTA)
    }

    @Test
    fun `exponential average leans on the fresh candle harder than the simple one`() {
        val closes = List(20) { 10.0 } + 20.0
        val exponential = candles(closes).movingAverage(ChartIndicator.EMA_20)
        val simple = candles(closes).movingAverage(ChartIndicator.SMA_20)

        assertEquals(10.0, exponential[19]!!, DELTA)
        // 20 * 2/21 + 10 * 19/21
        assertEquals(10.952381, exponential[20]!!, DELTA)
        assertTrue(exponential[20]!! > simple[20]!!)
    }

    @Test
    fun `the exponential average starts from the simple one, not from the first candle`() {
        // старт от одной цены даёт крюк, который живёт десятки свечей
        val closes = (1..20).map { it.toDouble() }
        val series = candles(closes).movingAverage(ChartIndicator.EMA_20)

        assertEquals(10.5, series[19]!!, DELTA)
    }

    @Test
    fun `the series is as long as the candles`() {
        val series = candles(List(60) { 10.0 }).movingAverage(ChartIndicator.SMA_50)

        assertEquals(60, series.size)
        assertNull(series[48])
        assertEquals(10.0, series[49]!!, DELTA)
    }

    private fun candles(closes: List<Double>): List<Candle> =
        closes.mapIndexed { index, close ->
            Candle(
                timeMillis = index * 60_000L,
                open = close,
                high = close,
                low = close,
                close = close,
            )
        }

    private companion object {
        const val DELTA = 1e-6
    }
}
