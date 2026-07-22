package com.cryptocompare.model.ticker

sealed class TickerConnectionState {
    object Connecting : TickerConnectionState()

    object Connected : TickerConnectionState()

    object Disconnected : TickerConnectionState()

    data class Reconnecting(
        val attempts: Int,
        val timeDelay: Long,
    ) : TickerConnectionState()

    data class Error(
        val errorMsg: String,
    ) : TickerConnectionState()
}
