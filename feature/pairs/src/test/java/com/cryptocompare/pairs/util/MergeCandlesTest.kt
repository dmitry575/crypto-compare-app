package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MergeCandlesTest {
    private fun candle(
        timeMillis: Long,
        close: Double = 1.0,
    ) = Candle(timeMillis = timeMillis, open = close, high = close, low = close, close = close)

    @Test
    fun `older page is prepended and the whole result is sorted by time`() {
        val existing = listOf(candle(3000L), candle(4000L))
        val older = listOf(candle(1000L), candle(2000L))

        val merged = mergeOlderCandles(existing, older)

        assertEquals(listOf(1000L, 2000L, 3000L, 4000L), merged.map { it.timeMillis })
    }

    @Test
    fun `duplicate times are deduplicated and the existing candle wins`() {
        // граница страниц пересеклась: свеча 3000 есть и там, и там — оставляем
        // уже показанную (в ней мог осесть живой тик)
        val existing = listOf(candle(3000L, close = 99.0))
        val older = listOf(candle(2000L), candle(3000L, close = 1.0))

        val merged = mergeOlderCandles(existing, older)

        assertEquals(listOf(2000L, 3000L), merged.map { it.timeMillis })
        assertEquals(99.0, merged.last().close, 0.0)
    }

    @Test
    fun `an empty older page returns the same list untouched`() {
        val existing = listOf(candle(3000L))

        assertSame(existing, mergeOlderCandles(existing, emptyList()))
    }
}
