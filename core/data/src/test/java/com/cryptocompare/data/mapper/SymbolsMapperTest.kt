package com.cryptocompare.data.mapper

import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolBestPriceDto
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolsMapperTest {
    @Test
    fun `sides keep their exchange on the way from dto to entity`() {
        // перепутать ask и bid здесь дешевле всего: на проводе они называются
        // priceBuy и priceSell, а в разбивке по биржам эти имена значат обратное
        val entity = bestPriceDto().toEntityFromDto(syncedAtMillis = SYNCED_AT)

        assertEquals(101.0, entity.bestAskPrice, 0.0001)
        assertEquals(18, entity.bestAskProviderId)
        assertEquals(100.0, entity.bestBidPrice, 0.0001)
        assertEquals(3, entity.bestBidProviderId)
    }

    @Test
    fun `spread and quote times survive the trip from dto to entity`() {
        val entity = bestPriceDto().toEntityFromDto(syncedAtMillis = SYNCED_AT)

        assertEquals(-0.99, entity.spreadPercent!!, 0.0001)
        assertEquals(UPDATED_AT, entity.bestAskUpdatedAt)
        assertEquals(UPDATED_AT, entity.bestBidUpdatedAt)
        assertEquals(SYNCED_AT, entity.syncedAtMillis)
    }

    @Test
    fun `24h stats survive the trip from dto to entity`() {
        val entity = bestPriceDto().toEntityFromDto(syncedAtMillis = SYNCED_AT)

        assertEquals(1_250.5, entity.volume24h!!, 0.0001)
        assertEquals(98_750_000.0, entity.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, entity.change24h!!, 0.0001)
    }

    @Test
    fun `a row without a spread keeps it null rather than zero`() {
        val entity = bestPriceDto(spreadPercent = null).toEntityFromDto(syncedAtMillis = SYNCED_AT)

        assertNull(entity.spreadPercent)
    }

    @Test
    fun `24h stats survive the trip from dto straight to domain`() {
        val symbol = dto().symbolToDomainFromDto()

        assertEquals(1_250.5, symbol.volume24h!!, 0.0001)
        assertEquals(98_750_000.0, symbol.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, symbol.change24h!!, 0.0001)
    }

    @Test
    fun `an exchange that reports no 24h stats keeps them null`() {
        // поля необязательные: подставлять ноль нельзя, иначе «нет данных»
        // на экране превратится в «объём нулевой»
        val symbol = dto(volume24h = null, quoteVolume24h = null, change24h = null).symbolToDomainFromDto()

        assertNull(symbol.volume24h)
        assertNull(symbol.quoteVolume24h)
        assertNull(symbol.change24h)
    }

    /** Строка каталога: лучшая пара цен, стороны с разных бирж. */
    private fun bestPriceDto(spreadPercent: Double? = -0.99) =
        SymbolBestPriceDto(
            id = 1,
            ticker = TICKER,
            symbol = TICKER,
            bestAskProviderId = 18,
            bestAskPrice = 101.0,
            bestBidProviderId = 3,
            bestBidPrice = 100.0,
            spreadPercent = spreadPercent,
            bestAskUpdatedAt = UPDATED_AT,
            bestBidUpdatedAt = UPDATED_AT,
            updatedAt = UPDATED_AT,
            volume24h = 1_250.5,
            quoteVolume24h = 98_750_000.0,
            change24h = 2.35,
        )

    /** Котировка одной биржи: здесь priceSell это ask, а priceBuy — bid. */
    private fun dto(
        volume24h: Double? = 1_250.5,
        quoteVolume24h: Double? = 98_750_000.0,
        change24h: Double? = 2.35,
    ) = SymbolDto(
        id = 1,
        ticker = TICKER,
        symbol = TICKER,
        providerId = 10,
        priceSell = 101.0,
        priceBuy = 100.0,
        updatedAt = UPDATED_AT,
        volume24h = volume24h,
        quoteVolume24h = quoteVolume24h,
        change24h = change24h,
    )

    private companion object {
        const val TICKER = "BTCUSDT"
        const val UPDATED_AT = "2026-09-03T11:00:44Z"
        const val SYNCED_AT = 1_700_000_000_000L
    }
}
