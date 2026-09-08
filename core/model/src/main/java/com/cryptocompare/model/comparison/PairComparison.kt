package com.cryptocompare.model.comparison

import com.cryptocompare.model.provider.ProviderDetail

/**
 * Сравнение котировок одной пары по биржам — то, ради чего приложение и существует.
 *
 * Лучшие цены **берутся с бэкенда, а не выбираются здесь**. Разбивка по биржам
 * приходит без фильтра свежести: на btcusdt 2026-09-08 у биржи с самым высоким
 * бидом котировка стояла два часа, и любой честный `maxByOrNull` вручил бы ей
 * победу. Бэкенд такие отбрасывает, поэтому [bestAskProviderId] и
 * [bestBidProviderId] приходят от него, а список [quotes] показывается как есть —
 * пользователю видно всё, но выделено то, чему можно верить.
 */
data class PairComparison(
    val ticker: String,
    /** Все биржи с котировкой, от самой дешёвой покупки к дорогой. */
    val quotes: List<ProviderDetail>,
    /** Биржа с лучшим ask: там пользователь покупает. `null` — бэкенд не выбрал. */
    val bestAskProviderId: Int?,
    /** Лучший ask среди бирж — самая низкая цена покупки. */
    val bestAskPrice: Double?,
    /** Биржа с лучшим bid: там пользователь продаёт. */
    val bestBidProviderId: Int?,
    /** Лучший bid среди бирж — самая высокая цена продажи. */
    val bestBidPrice: Double?,
    /** Спред в процентах от цены покупки, со знаком. В норме отрицателен. */
    val spreadPercent: Double?,
) {
    /** Разница в котируемом активе: плюс — заработок, минус — потеря на паре. */
    val difference: Double?
        get() =
            if (bestAskPrice != null && bestBidPrice != null) {
                bestBidPrice - bestAskPrice
            } else {
                null
            }

    /** Сравнивать не с чем: пара торгуется на одной бирже. */
    val isSingleExchange: Boolean
        get() = quotes.size == 1
}
