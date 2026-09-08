package com.cryptocompare.model.ticker

/**
 * Лучшая пара цен по тикеру: где дешевле купить и где дороже продать.
 *
 * Стороны с разных бирж, поэтому идентификаторов два. Не путать с [TickerPrice] —
 * там котировка **одной** биржи, и в каталог она не пишется: `symbolId` у всех
 * бирж тикера общий, так что такой тик затирал бы лучшую пару ценами той биржи,
 * которая тикнула последней.
 */
data class TickerBestPrice(
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
    /** Спред в процентах от цены покупки, со знаком. Считает бэкенд. */
    val spreadPercent: Double?,
)
