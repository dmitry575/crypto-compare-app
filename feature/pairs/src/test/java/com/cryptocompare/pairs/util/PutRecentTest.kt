package com.cryptocompare.pairs.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PutRecentTest {
    @Test
    fun `the map never holds more than the limit`() {
        val charts = mutableMapOf<String, Int>()

        listOf("binance-1d", "bybit-1d", "okx-1d", "htx-1d").forEachIndexed { index, key ->
            charts.putRecent(key, index, maxSize = 3)
        }

        assertEquals(3, charts.size)
    }

    @Test
    fun `the longest untouched key goes first`() {
        val charts = mutableMapOf<String, Int>()
        charts.putRecent("binance-1d", 1, maxSize = 3)
        charts.putRecent("bybit-1d", 2, maxSize = 3)
        charts.putRecent("okx-1d", 3, maxSize = 3)

        charts.putRecent("htx-1d", 4, maxSize = 3)

        assertEquals(listOf("bybit-1d", "okx-1d", "htx-1d"), charts.keys.toList())
    }

    @Test
    fun `touching a key again saves it from eviction`() {
        // график, к которому вернулись, свежий — вытеснять надо другой
        val charts = mutableMapOf<String, Int>()
        charts.putRecent("binance-1d", 1, maxSize = 3)
        charts.putRecent("bybit-1d", 2, maxSize = 3)
        charts.putRecent("okx-1d", 3, maxSize = 3)

        charts.putRecent("binance-1d", 10, maxSize = 3)
        charts.putRecent("htx-1d", 4, maxSize = 3)

        assertEquals(listOf("okx-1d", "binance-1d", "htx-1d"), charts.keys.toList())
        assertEquals(10, charts["binance-1d"])
    }
}
