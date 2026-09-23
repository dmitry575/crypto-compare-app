package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.dao.PortfolioPositionWithExchange
import com.cryptocompare.data.local.dao.SymbolSellPrice
import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import com.cryptocompare.model.portfolio.PortfolioQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioMapperTest {
    @Test
    fun `a position keeps its exchange on the way out of the database`() {
        val position = PortfolioPositionWithExchange(entity(providerId = 5), exchangeName = "bybit").toDomain()

        assertEquals(5, position.providerId)
        assertEquals("bybit", position.exchangeName)
    }

    @Test
    fun `a position from before the exchange field has none`() {
        val position = PortfolioPositionWithExchange(entity(providerId = null), exchangeName = null).toDomain()

        assertNull(position.providerId)
        assertNull(position.exchangeName)
    }

    @Test
    fun `a blank exchange name is no name`() {
        // строка «на » в портфеле хуже, чем имя, которого нет
        val position = PortfolioPositionWithExchange(entity(providerId = 5), exchangeName = " ").toDomain()

        assertNull(position.exchangeName)
    }

    @Test
    fun `the exchange survives the way back into the database`() {
        val domain = PortfolioPositionWithExchange(entity(providerId = 5), exchangeName = "bybit").toDomain()

        assertEquals(entity(providerId = 5), domain.toEntity())
    }

    @Test
    fun `a stored quote keeps its exchange and time`() {
        val entity = PortfolioQuote(symbolId = 1L, providerId = 5, price = 81_050.0, quotedAtMillis = 42L).toEntity()

        assertEquals(1L, entity.symbolId)
        assertEquals(5, entity.providerId)
        assertEquals(81_050.0, entity.price, 0.0)
        assertEquals(42L, entity.quotedAtMillis)
    }

    @Test
    fun `sell quotes without a usable price are left out`() {
        // ноль в портфеле читался бы как «позиция обесценилась»
        val quotes =
            listOf(
                SymbolSellPrice(symbolId = 1L, sellPrice = 81_050.0, providerId = 5, providerName = "bybit"),
                SymbolSellPrice(symbolId = 2L, sellPrice = 0.0, providerId = 5, providerName = "bybit"),
                SymbolSellPrice(symbolId = 3L, sellPrice = Double.NaN, providerId = 5, providerName = "bybit"),
            ).toSellQuotes()

        assertEquals(setOf(1L), quotes.keys)
        assertEquals("bybit", quotes.getValue(1L).exchangeName)
    }

    private fun entity(providerId: Int?) =
        PortfolioPositionEntity(
            symbolId = 1L,
            ticker = "BTCUSDT",
            amount = 0.42,
            buyPrice = 72_000.0,
            updatedAtMillis = 1_700_000_000_000L,
            providerId = providerId,
        )
}
