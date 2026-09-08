package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

import com.google.gson.annotations.SerializedName

/**
 * Строка каталога: лучшая пара цен по тикеру, сведённая бэкендом по всем биржам.
 *
 * На проводе стороны названы `priceSell` и `priceBuy`, и это ловушка: в разбивке
 * по биржам (`/v1/symbols/ticker/{ticker}`) те же имена означают обратное —
 * там `priceSell` это ask одной биржи. Поэтому здесь берутся однозначные имена
 * `bestAskPrice` / `bestBidPrice`, которые бэкенд отдаёт рядом с теми же
 * значениями специально для этого. `@SerializedName` доклеивает к ним отметки
 * времени, которые однозначных имён не получили.
 *
 * Ask — цена, по которой покупает пользователь; bid — по которой продаёт.
 */
data class SymbolBestPriceDto(
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
    /** `(bestBidPrice - bestAskPrice) / bestAskPrice * 100`, считает бэкенд. */
    val spreadPercent: Double?,
    @SerializedName("priceBuyUpdatedAt")
    val bestAskUpdatedAt: String?,
    @SerializedName("priceSellUpdatedAt")
    val bestBidUpdatedAt: String?,
    /** Свежесть строки целиком: позднее из двух времён. */
    val updatedAt: String?,
    val volume24h: Double? = null,
    val quoteVolume24h: Double? = null,
    val change24h: Double? = null,
)
