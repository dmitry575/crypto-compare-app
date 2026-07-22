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
    ) : ConnectionState()
}
