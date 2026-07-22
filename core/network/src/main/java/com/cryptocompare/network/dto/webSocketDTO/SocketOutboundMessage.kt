package com.cryptocompare.network.dto.webSocketDTO

import com.cryptocompare.network.dto.webSocketDTO.dataTypes.TickerData

data class SocketOutboundMessage(
    val type: Int,
    val data: TickerData,
)
