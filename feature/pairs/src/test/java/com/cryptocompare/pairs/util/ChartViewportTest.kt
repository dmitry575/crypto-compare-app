package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartViewportTest {
    /** Свечи одной пары: время растёт вместе с индексом, по нему их и различаем. */
    private fun candles(
        count: Int,
        startTime: Long = 0L,
    ) = List(count) { index ->
        val time = startTime + index * STEP_MS
        Candle(timeMillis = time, open = 1.0, high = 2.0, low = 0.5, close = 1.5)
    }

    private fun ChartHistory.visibleTimes(viewport: ChartViewport): List<Long> =
        viewport
            .visibleIndices(candles.size, serverCount)
            .map { candles[it].timeMillis }

    @Test
    fun `a fresh frame ends on the newest candle`() {
        val viewport = freshEdgeViewport(newestAbs = 0, visibleCount = 60)

        assertEquals(1f, viewport.rightEdge, 0f)
        assertEquals(-59f, viewport.leftEdge, 0f)
    }

    @Test
    fun `scrolling left walks into history`() {
        val viewport = freshEdgeViewport(newestAbs = 0, visibleCount = 60).scrolledBy(-10f)

        assertEquals(-69f, viewport.leftEdge, 0f)
        assertEquals(60f, viewport.visibleCount, 0f)
    }

    @Test
    fun `zoom keeps the candle under the fingers in place`() {
        val viewport = ChartViewport(leftEdge = -100f, visibleCount = 60f)
        // палец посередине кадра — там свеча -70
        val zoomed = viewport.zoomedBy(factor = 2f, focusFraction = 0.5f)

        assertEquals(30f, zoomed.visibleCount, 0.001f)
        assertEquals(-70f, zoomed.leftEdge + zoomed.visibleCount * 0.5f, 0.001f)
    }

    @Test
    fun `the frame cannot leave the loaded range`() {
        // загружено 100 свечей: абсолютные индексы -99..0
        val far = ChartViewport(leftEdge = -500f, visibleCount = 60f).clampedTo(-99, 0)
        assertEquals(-99f, far.leftEdge, 0f)

        val future = ChartViewport(leftEdge = 500f, visibleCount = 60f).clampedTo(-99, 0)
        assertEquals(1f, future.rightEdge, 0f)
    }

    @Test
    fun `zooming out stops at the loaded depth`() {
        // отдалять дальше загруженного нельзя — иначе в кадре была бы пустота
        val zoomed = ChartViewport(leftEdge = -99f, visibleCount = 300f).clampedTo(-99, 0)

        assertEquals(100f, zoomed.visibleCount, 0f)
        assertEquals(-99f, zoomed.leftEdge, 0f)
    }

    @Test
    fun `a loaded page does not move the frame`() {
        // главное свойство: кадр считается в абсолютных индексах, поэтому дописанная
        // слева страница его не двигает — компенсировать сдвиг не нужно
        val history = ChartHistory.initial(candles(count = 100, startTime = 100 * STEP_MS))
        val before =
            freshEdgeViewport(history.newestAbs)
                .scrolledBy(-30f)
                .clampedTo(history.oldestAbs, history.newestAbs)

        val grown = history.withOlderPage(candles(count = 100, startTime = 0L))
        val after = before.clampedTo(grown.oldestAbs, grown.newestAbs)

        assertEquals(before, after)
        // и на экране те же самые свечи, а не соседние
        assertEquals(history.visibleTimes(before), grown.visibleTimes(after))
    }

    @Test
    fun `the next page is asked for before the edge is reached`() {
        val viewport = ChartViewport(leftEdge = -99f, visibleCount = 60f)

        assertTrue("у самого края страницу не просят", viewport.needsOlderPage(oldestAbs = -99, marginScreens = 1f))
        assertFalse(
            "страницу просят слишком рано",
            viewport.needsOlderPage(oldestAbs = -300, marginScreens = 1f),
        )
    }

    @Test
    fun `visible indices cover the frame with a candle to spare`() {
        // 100 загруженных свечей, живых баров нет: индекс списка = абсолютный + 99
        val indices = ChartViewport(leftEdge = -50f, visibleCount = 10f).visibleIndices(size = 100, serverCount = 100)

        assertEquals(49, indices.first)
        assertEquals(59, indices.last)
    }

    private companion object {
        const val STEP_MS = 60_000L
    }
}
