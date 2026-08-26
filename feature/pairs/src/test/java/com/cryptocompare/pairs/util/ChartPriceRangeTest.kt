package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToLong

class ChartPriceRangeTest {
    private fun candle(
        low: Double,
        high: Double,
    ) = Candle(timeMillis = 0L, open = low, high = high, low = low, close = high)

    private fun flatSeries(
        size: Int,
        low: Double,
        high: Double,
    ) = List(size) { candle(low, high) }

    /** Границы обязаны быть кратны шагу — иначе подписи оси встают на «некруглых» ценах. */
    private fun assertAlignedToStep(range: ChartPriceRange) {
        listOf(range.min, range.max).forEach { bound ->
            val steps = bound / range.step
            assertTrue(
                "граница $bound не кратна шагу ${range.step}",
                abs(steps - steps.roundToLong()) < 1e-6,
            )
        }
    }

    @Test
    fun `range covers the visible candles with room above and below`() {
        val candles = flatSeries(size = 20, low = 100.0, high = 110.0)

        val range = candles.chartPriceRange(0.0..19.0)

        assertTrue("минимум оси выше свечей: ${range.min}", range.min < 100.0)
        assertTrue("максимум оси ниже свечей: ${range.max}", range.max > 110.0)
        assertAlignedToStep(range)
    }

    @Test
    fun `a spike outside the frame does not squash the visible candles`() {
        // памп в начале истории: если считать ось по всему ряду, текущие свечи
        // превращаются в ленту у нижнего края
        val candles = listOf(candle(10.0, 1000.0)) + flatSeries(size = 20, low = 100.0, high = 110.0)

        val range = candles.chartPriceRange(1.0..20.0)

        assertTrue("ось растянута на памп: ${range.max}", range.max < 200.0)
        assertTrue(range.min < 100.0 && range.max > 110.0)
    }

    @Test
    fun `the frame is clamped to the series when vico overshoots its edges`() {
        // vico отдаёт диапазон с запасом за края данных
        val candles = flatSeries(size = 5, low = 100.0, high = 110.0)

        val range = candles.chartPriceRange(-3.0..40.0)

        assertTrue(range.min < 100.0 && range.max > 110.0)
        assertAlignedToStep(range)
    }

    @Test
    fun `a flat frame still gets a readable range`() {
        val candles = flatSeries(size = 10, low = 50.0, high = 50.0)

        val range = candles.chartPriceRange(0.0..9.0)

        assertTrue("окно схлопнулось", range.max > range.min)
        assertTrue(range.step > 0.0)
        assertTrue(range.min <= 50.0 && range.max >= 50.0)
    }

    @Test
    fun `sub-cent prices get a step of their own order`() {
        val candles = flatSeries(size = 10, low = 0.000_012_0, high = 0.000_013_0)

        val range = candles.chartPriceRange(0.0..9.0)

        assertTrue("шаг крупнее самой цены: ${range.step}", range.step < 0.000_001)
        assertTrue(range.min < 0.000_012_0 && range.max > 0.000_013_0)
        assertAlignedToStep(range)
    }

    @Test
    fun `without a frame the range follows the newest candles`() {
        // до первой отрисовки видимого диапазона ещё нет, а график открывается
        // на свежем крае — по нему и считаем
        val old = flatSeries(size = 100, low = 10.0, high = 12.0)
        val fresh = flatSeries(size = PairsConstants.Chart.VISIBLE_CANDLES, low = 100.0, high = 110.0)
        val candles = old + fresh

        val range = candles.chartPriceRange(visibleXRange = null)

        assertTrue("ось ушла к старым свечам: ${range.min}", range.min > 50.0)
        assertTrue(range.max > 110.0)
    }

    @Test
    fun `an empty series gets a neutral range instead of a broken axis`() {
        val range = emptyList<Candle>().chartPriceRange(0.0..10.0)

        assertEquals(0.0, range.min, 0.0)
        assertTrue(range.max > range.min)
        assertTrue(range.step > 0.0)
    }
}
