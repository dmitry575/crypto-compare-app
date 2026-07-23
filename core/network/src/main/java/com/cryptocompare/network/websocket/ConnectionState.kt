package com.cryptocompare.network.websocket

sealed class ConnectionState {
    object Connecting : ConnectionState()

    object Connected : ConnectionState()

    object Disconnected : ConnectionState()

    data class Reconnecting(
        val attempts: Int,
        val timeDelay: Long,
    ) : ConnectionState()

    data class Error(
        val errorMsg: String,
        /**
         * Настоящее исключение, если оно было. Наружу (в TickerConnectionState)
         * не идёт — только errorMsg, — но core:data логирует по нему non-fatal,
         * чтобы Crashlytics группировал сбои по типу, а не по одной строке.
         */
        val cause: Throwable? = null,
    ) : ConnectionState()
}
