package com.cryptocompare.network.dto.webSocketDTO

import com.google.gson.JsonElement

data class SocketInboundRawMessage(
    val id: String,
    val type: Int,
    val data: JsonElement,
)
