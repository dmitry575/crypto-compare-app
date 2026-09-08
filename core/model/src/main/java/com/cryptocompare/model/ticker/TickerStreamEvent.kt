package com.cryptocompare.model.ticker

sealed interface TickerStreamEvent {
    val id: String

    data class Welcome(
        override val id: String,
        val message: String,
    ) : TickerStreamEvent

    data class Subscribe(
        override val id: String,
        val ticker: String,
    ) : TickerStreamEvent

    data class Unsubscribe(
        override val id: String,
        val ticker: String,
    ) : TickerStreamEvent

    data class TickerPriceChange(
        override val id: String,
        val data: TickerPrice,
    ) : TickerStreamEvent

    /**
     * Лучшая пара цен по тикеру. Каталог обновляется только этим событием:
     * [TickerPriceChange] несёт котировку одной биржи и лучшую пару затирает.
     */
    data class TickerBestPriceChange(
        override val id: String,
        val data: TickerBestPrice,
    ) : TickerStreamEvent

    data class Error(
        override val id: String,
        val errorCode: Int,
        val error: String,
    ) : TickerStreamEvent
}
