package com.cryptocompare.data

import com.cryptocompare.data.mapper.toKlineInterval
import com.cryptocompare.model.chart.ChartTimeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTimeframeMapperTest {
    @Test
    fun `each timeframe maps to the backend interval string`() {
        assertEquals("15m", ChartTimeframe.M15.toKlineInterval())
        assertEquals("1h", ChartTimeframe.H1.toKlineInterval())
        assertEquals("4h", ChartTimeframe.H4.toKlineInterval())
        assertEquals("1d", ChartTimeframe.D1.toKlineInterval())
        assertEquals("1w", ChartTimeframe.W1.toKlineInterval())
    }

    @Test
    fun `every timeframe has a non-blank interval`() {
        ChartTimeframe.entries.forEach { timeframe ->
            assertTrue("$timeframe has no interval", timeframe.toKlineInterval().isNotBlank())
        }
    }
}
