package com.cryptocompare.network.dto.webSocketDTO.dataTypes

/**
 * Сообщение типа 5: лучшая пара цен по тикеру изменилась.
 *
 * Той же формы, что строка `GET /v1/symbols`, поэтому кладётся в каталог как
 * есть — приложение ничего не агрегирует. В отличие от типа 4, который несёт
 * котировку одной биржи и в каталог попадать не должен: `symbolId` там общий
 * на весь тикер, и такой тик затирал бы лучшую пару ценами случайной биржи.
 *
 * На проводе стороны продублированы под именами `priceBuy` / `priceSell`;
 * берём однозначные, где видно, ask это или bid.
 */
data class SymbolBestPriceChangeData(
    val ticker: String,
    val symbolId: Long,
    /** Биржа с лучшим ask: там пользователь покупает. */
    val bestAskProviderId: Int?,
    /** Лучший ask среди бирж — самая низкая цена покупки. */
    val bestAskPrice: Double,
    /** Биржа с лучшим bid: там пользователь продаёт. */
    val bestBidProviderId: Int?,
    /** Лучший bid среди бирж — самая высокая цена продажи. */
    val bestBidPrice: Double,
    val spreadPercent: Double?,
)
