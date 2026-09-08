package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Строка каталога: тикер и лучшая пара цен по нему, сведённая бэкендом.
 *
 * Ask — цена, по которой покупает пользователь, bid — по которой продаёт;
 * биржи у них разные, отсюда два `providerId`. Имена сторон однозначные
 * намеренно: на проводе те же величины зовутся `priceBuy` и `priceSell`,
 * а в разбивке по биржам эти имена означают обратное.
 *
 * Внешнего ключа на `providers` больше нет. Он был один, на несуществующее
 * теперь поле `providerId`, и держался костылём: строкам каталога
 * проставлялась первая биржа подряд, лишь бы ключ не падал. Биржа из строки
 * вполне может ещё не лежать в справочнике, и это не повод терять котировку.
 */
@Entity(
    tableName = "symbols",
    indices = [
        Index(value = ["bestAskProviderId"]),
        Index(value = ["bestBidProviderId"]),
    ],
)
data class SymbolEntity(
    @PrimaryKey
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    /** Биржа с лучшим ask: там пользователь покупает. */
    val bestAskProviderId: Int?,
    /** Лучший ask среди бирж — самая низкая цена покупки. */
    val bestAskPrice: Double,
    /** Биржа с лучшим bid: там пользователь продаёт. */
    val bestBidProviderId: Int?,
    /** Лучший bid среди бирж — самая высокая цена продажи. */
    val bestBidPrice: Double,
    /**
     * Спред в процентах от цены покупки, со знаком. Считает бэкенд, приложение
     * его не пересчитывает. `null` — считать не из чего.
     */
    val spreadPercent: Double?,
    /** Когда лучший ask пришёл со своей биржи. */
    val bestAskUpdatedAt: String?,
    /** Когда лучший bid пришёл со своей биржи. */
    val bestBidUpdatedAt: String?,
    /** Свежесть строки целиком: позднее из двух времён. */
    val updatedAt: String,
    val syncedAtMillis: Long,
    val volume24h: Double? = null,
    val quoteVolume24h: Double? = null,
    val change24h: Double? = null,
)
