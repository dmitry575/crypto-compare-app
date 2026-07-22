package com.cryptocompare.data

import com.cryptocompare.data.mapper.toRequestSpec
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.chart.HistoryResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTimeframeMapperTest {
    @Test
    fun `intraday timeframes map to minute and hour series`() {
        val m15 = ChartTimeframe.M15.toRequestSpec()
        assertEquals(HistoryResolution.MINUTE, m15.resolution)
        assertEquals(15, m15.aggregate)

        val h1 = ChartTimeframe.H1.toRequestSpec()
        assertEquals(HistoryResolution.HOUR, h1.resolution)
        assertEquals(1, h1.aggregate)

        val h4 = ChartTimeframe.H4.toRequestSpec()
        assertEquals(HistoryResolution.HOUR, h4.resolution)
        assertEquals(4, h4.aggregate)
    }

    @Test
    fun `daily and weekly map to day series with aggregation`() {
        val d1 = ChartTimeframe.D1.toRequestSpec()
        assertEquals(HistoryResolution.DAY, d1.resolution)
        assertEquals(1, d1.aggregate)

        val w1 = ChartTimeframe.W1.toRequestSpec()
        assertEquals(HistoryResolution.DAY, w1.resolution)
        assertEquals(7, w1.aggregate)
    }

    @Test
    fun `every timeframe asks for a depth within the api limit`() {
        ChartTimeframe.entries.forEach { timeframe ->
            val limit = timeframe.toRequestSpec().limit
            assertTrue("$timeframe requests $limit candles", limit in 1..MIN_API_MAX_LIMIT)
        }
    }

    @Test
    fun `daily timeframe covers about a year`() {
        assertEquals(365, ChartTimeframe.D1.toRequestSpec().limit)
    }
}

/** Потолок min-api на число точек в одном запросе. */
private const val MIN_API_MAX_LIMIT = 2000
