package com.cryptocompare.data.mapper

import com.cryptocompare.model.symbol.PairAggregateRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairAggregateMapperTest {
    private fun row(
        symbolIds: String? = "1,2",
        providerIds: String? = "10,20",
        minPrice: Double = 100.0,
        maxPrice: Double = 110.0,
        spreadPercent: Double = 10.0,
        quoteVolume24h: Double? = 98_750_000.0,
        change24h: Double? = 2.35,
    ) = PairAggregateRow(
        ticker = "BTCUSDT",
        symbolIds = symbolIds,
        providerIds = providerIds,
        minPrice = minPrice,
        maxPrice = maxPrice,
        spreadPercent = spreadPercent,
        quoteVolume24h = quoteVolume24h,
        change24h = change24h,
    )

    @Test
    fun `concatenated ids become lists`() {
        val item = row().toPairUiItem()

        assertEquals(listOf(1L, 2L), item.symbolIds)
        assertEquals(listOf(10, 20), item.providerIds)
    }

    @Test
    fun `spread comes through from the query`() {
        // считается в SQL, слой представления его не пересчитывает
        assertEquals(10.0, row(spreadPercent = 10.0).toPairUiItem().spreadPercent, 0.0001)
    }

    @Test
    fun `repeated providers collapse to distinct ones`() {
        val item = row(providerIds = "10,10,20").toPairUiItem()

        assertEquals(listOf(10, 20), item.providerIds)
    }

    @Test
    fun `providers without an id are dropped`() {
        // нулевой providerId в базе означает «биржа неизвестна»
        val item = row(providerIds = "0,10,-1").toPairUiItem()

        assertEquals(listOf(10), item.providerIds)
    }

    @Test
    fun `missing ids give empty lists rather than a crash`() {
        val item = row(symbolIds = null, providerIds = null).toPairUiItem()

        assertTrue(item.symbolIds.isEmpty())
        assertTrue(item.providerIds.isEmpty())
    }

    @Test
    fun `garbage in the concatenation is skipped`() {
        val item = row(symbolIds = "1,,abc,2").toPairUiItem()

        assertEquals(listOf(1L, 2L), item.symbolIds)
    }

    @Test
    fun `24h stats pass through untouched`() {
        val item = row().toPairUiItem()

        assertEquals(98_750_000.0, item.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, item.change24h!!, 0.0001)
    }

    @Test
    fun `missing 24h stats stay null`() {
        val item = row(quoteVolume24h = null, change24h = null).toPairUiItem()

        assertNull(item.quoteVolume24h)
        assertNull(item.change24h)
    }
}
