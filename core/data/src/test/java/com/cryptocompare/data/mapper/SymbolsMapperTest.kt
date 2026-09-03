package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolsMapperTest {
    @Test
    fun `24h stats survive the trip from dto to entity`() {
        val entity = dto().toEntityFromDto(syncedAtMillis = SYNCED_AT)

        assertEquals(1_250.5, entity.volume24h!!, 0.0001)
        assertEquals(98_750_000.0, entity.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, entity.change24h!!, 0.0001)
        assertEquals(SYNCED_AT, entity.syncedAtMillis)
    }

    @Test
    fun `24h stats survive the trip from entity to domain`() {
        val symbol = entity().toDomainFromEntity()

        assertEquals(1_250.5, symbol.volume24h!!, 0.0001)
        assertEquals(98_750_000.0, symbol.quoteVolume24h!!, 0.0001)
        assertEquals(2.35, symbol.change24h!!, 0.0001)
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

    private fun entity() =
        SymbolEntity(
            id = 1,
            ticker = TICKER,
            symbol = TICKER,
            providerId = 10,
            priceSell = 101.0,
            priceBuy = 100.0,
            updatedAt = UPDATED_AT,
            syncedAtMillis = SYNCED_AT,
            volume24h = 1_250.5,
            quoteVolume24h = 98_750_000.0,
            change24h = 2.35,
        )

    private companion object {
        const val TICKER = "BTCUSDT"
        const val UPDATED_AT = "2026-09-03T11:00:44Z"
        const val SYNCED_AT = 1_700_000_000_000L
    }
}
