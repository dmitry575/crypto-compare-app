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

    private fun series(
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
    fun `range covers the candles with room above and below`() {
        val range = series(size = 20, low = 100.0, high = 110.0).chartPriceRange()

        assertTrue("минимум оси выше свечей: ${range.min}", range.min < 100.0)
        assertTrue("максимум оси ниже свечей: ${range.max}", range.max > 110.0)
        assertAlignedToStep(range)
    }

    @Test
    fun `a spike outside the frame does not squash the visible candles`() {
        // памп в начале истории: если считать ось по всему ряду, текущие свечи
        // превращаются в ленту у нижнего края
        val all = listOf(candle(10.0, 1000.0)) + series(size = 20, low = 100.0, high = 110.0)
        val frame = ChartViewport(leftEdge = -19f, visibleCount = 20f)

        val visible = frame.visibleIndices(all.size, serverCount = all.size)
        val range = all.subList(visible.first, visible.last + 1).chartPriceRange()

        assertTrue("ось растянута на памп: ${range.max}", range.max < 200.0)
        assertTrue(range.min < 100.0 && range.max > 110.0)
    }

    @Test
    fun `a flat frame still gets a readable range`() {
        val range = series(size = 10, low = 50.0, high = 50.0).chartPriceRange()

        assertTrue("окно схлопнулось", range.max > range.min)
        assertTrue(range.step > 0.0)
        assertTrue(range.min <= 50.0 && range.max >= 50.0)
    }

    @Test
    fun `sub-cent prices get a step of their own order`() {
        val range = series(size = 10, low = 0.000_012_0, high = 0.000_013_0).chartPriceRange()

        assertTrue("шаг крупнее самой цены: ${range.step}", range.step < 0.000_001)
        assertTrue(range.min < 0.000_012_0 && range.max > 0.000_013_0)
        assertAlignedToStep(range)
    }

    @Test
    fun `an empty frame gets a neutral range instead of a broken axis`() {
        val range = emptyList<Candle>().chartPriceRange()

        assertEquals(0.0, range.min, 0.0)
        assertTrue(range.max > range.min)
        assertTrue(range.step > 0.0)
    }

    @Test
    fun `labels run from the bottom of the axis to the top with the same step`() {
        val range = series(size = 5, low = 100.0, high = 110.0).chartPriceRange()

        val labels = range.priceLabels()

        assertEquals(range.min, labels.first(), 1e-9)
        assertEquals(range.max, labels.last(), 1e-9)
        labels.zipWithNext { low, high -> assertEquals(range.step, high - low, 1e-9) }
    }
}
