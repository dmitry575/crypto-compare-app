package com.cryptocompare.network.websocket

enum class MessageType(
    val type: Int,
) {
    WELCOME(0),
    SUBSCRIBE(1),
    UNSUBSCRIBE(2),
    PRICE_CHANGE(4),
    ERROR(-1),
    ;

    companion object {
        fun fromType(type: Int): MessageType = entries.firstOrNull { it.type == type } ?: ERROR
    }
}
