package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartWindowTest {
    private fun candle(
        timeMillis: Long,
        close: Double = 1.0,
    ) = Candle(timeMillis = timeMillis, open = close, high = close, low = close, close = close)

    private fun candles(vararg times: Long) = times.map { candle(it) }

    private fun times(window: ChartWindow) = window.candles.map { it.timeMillis }

    @Test
    fun `initial page sits at the live edge and can page older`() {
        val window = ChartWindow.initial(candles(10, 20, 30), windowMax = 10)

        assertEquals(listOf(10L, 20L, 30L), times(window))
        assertEquals(0, window.newestSkip)
        assertEquals(3, window.oldestSkip)
        assertTrue(window.canLoadOlder)
        assertFalse(window.canLoadNewer)
    }

    @Test
    fun `an empty initial page reports no history at all`() {
        val window = ChartWindow.initial(emptyList(), windowMax = 10)

        assertFalse(window.canLoadOlder)
        assertFalse(window.canLoadNewer)
    }

    @Test
    fun `older page is prepended and the oldest offset advances`() {
        val window = ChartWindow.initial(candles(10, 20, 30), windowMax = 10)

        val next = window.withOlderPage(candles(1, 2, 3), windowMax = 10)

        assertEquals(listOf(1L, 2L, 3L, 10L, 20L, 30L), times(next))
        assertEquals(0, next.newestSkip)
        assertEquals(6, next.oldestSkip)
        assertFalse(next.canLoadNewer)
    }

    @Test
    fun `older page over the window drops the newest edge and enables newer paging`() {
        val window = ChartWindow.initial(candles(10, 20, 30), windowMax = 4)

        val next = window.withOlderPage(candles(1, 2, 3), windowMax = 4)

        // окно едет: свежий край (20, 30) выгружен, осталось 4 свечи
        assertEquals(listOf(1L, 2L, 3L, 10L), times(next))
        assertEquals(2, next.newestSkip)
        assertEquals(6, next.oldestSkip)
        assertTrue(next.canLoadNewer)
        assertTrue(next.canLoadOlder)
    }

    @Test
    fun `an empty older page marks the oldest reached`() {
        val window = ChartWindow.initial(candles(10, 20, 30), windowMax = 10)

        val next = window.withOlderPage(emptyList(), windowMax = 10)

        assertFalse(next.canLoadOlder)
    }

    @Test
    fun `an older page of only duplicates marks the oldest reached`() {
        val window = ChartWindow.initial(candles(10, 20, 30), windowMax = 10)

        val next = window.withOlderPage(candles(10, 20), windowMax = 10)

        assertEquals(3, next.candles.size)
        assertFalse(next.canLoadOlder)
    }

    @Test
    fun `newer page returns toward the present, drops the oldest edge and reopens older paging`() {
        // окно уехало вглубь: держим [1,2,3,10], свежий край подрезан (newestSkip=2)
        val deep =
            ChartWindow
                .initial(
                    candles(10, 20, 30),
                    windowMax = 4,
                ).withOlderPage(candles(1, 2, 3), windowMax = 4)
        assertTrue(deep.canLoadNewer)

        // грузим 2 более свежие свечи (age 1 и 0) назад к настоящему
        val back = deep.withNewerPage(candles(20, 30), offset = 0, windowMax = 4)

        assertEquals(listOf(3L, 10L, 20L, 30L), times(back))
        assertEquals(0, back.newestSkip)
        assertEquals(4, back.oldestSkip)
        assertFalse(back.canLoadNewer)
        // старый край выгружен → глубже снова есть что грузить
        assertTrue(back.canLoadOlder)
    }

    @Test
    fun `initial page larger than the window keeps the newest candles`() {
        val window = ChartWindow.initial(candles(10, 20, 30, 40, 50), windowMax = 3)

        assertEquals(listOf(30L, 40L, 50L), times(window))
        assertEquals(0, window.newestSkip)
    }
}
