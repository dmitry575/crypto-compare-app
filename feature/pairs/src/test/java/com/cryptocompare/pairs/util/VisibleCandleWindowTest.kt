package com.cryptocompare.pairs.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleCandleWindowTest {
    /** 200 свечей истории, 50 в кадре, значит листать можно на 150 свечей. */
    private fun window(
        scrollValue: Float,
        candleCount: Int = 200,
        visibleCount: Int = 50,
        maxScrollValue: Float = 3000f,
    ) = visibleCandleWindow(candleCount, visibleCount, scrollValue, maxScrollValue)

    @Test
    fun `empty history yields an empty window`() {
        assertTrue(window(scrollValue = 0f, candleCount = 0).isEmpty())
    }

    @Test
    fun `history that fits the frame is fully visible`() {
        // maxScroll = 0: листать нечего, шкала считается по всем свечам
        val window = window(scrollValue = 0f, candleCount = 40, maxScrollValue = 0f)

        assertEquals(0..39, window)
    }

    @Test
    fun `scrolled to the start the window sits at the oldest candles`() {
        val window = window(scrollValue = 0f)

        assertEquals(0, window.first)
        assertEquals(50, window.last)
    }

    @Test
    fun `scrolled to the end the window sits at the freshest candles`() {
        val window = window(scrollValue = 3000f)

        assertEquals(199, window.last)
        // 150 — первая видимая свеча, минус запасная слева
        assertEquals(149, window.first)
    }

    @Test
    fun `window travels with the scroll position`() {
        val start = window(scrollValue = 0f)
        val middle = window(scrollValue = 1500f)
        val end = window(scrollValue = 3000f)

        assertTrue(start.first < middle.first)
        assertTrue(middle.first < end.first)

        // в середине истории размер окна от позиции не зависит: едет только начало.
        // У краёв окно на свечу уже — запасную свечу там обрезает граница истории.
        assertEquals(window(scrollValue = 1000f).count(), window(scrollValue = 2000f).count())
        assertEquals(start.count() + 1, middle.count())
        assertEquals(end.count() + 1, middle.count())
    }

    @Test
    fun `window never leaves the bounds of the history`() {
        // значения за пределами предела скролла приходят при overscroll
        val overscrolled = window(scrollValue = 9000f)
        val negative = window(scrollValue = -500f)

        assertTrue(overscrolled.first >= 0 && overscrolled.last <= 199)
        assertTrue(negative.first >= 0 && negative.last <= 199)
    }

    @Test
    fun `a frame wider than the history shows all of it`() {
        // на молодой паре свечей меньше, чем помещается в кадр
        val window = window(scrollValue = 0f, candleCount = 12, visibleCount = 50)

        assertEquals(0..11, window)
    }

    @Test
    fun `a single candle still gives a usable window`() {
        assertEquals(0..0, window(scrollValue = 0f, candleCount = 1))
    }
}
