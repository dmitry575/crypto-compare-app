package com.cryptocompare.network.dto.webSocketDTO

import com.cryptocompare.network.dto.webSocketDTO.dataTypes.ErrorData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.SymbolPriceChangeData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.TickerData
import com.cryptocompare.network.dto.webSocketDTO.dataTypes.WelcomeData

sealed interface SocketDtoMessage {
    val id: String

    data class Welcome(
        override val id: String,
        val data: WelcomeData,
    ) : SocketDtoMessage

    data class Subscribe(
        override val id: String,
        val data: TickerData,
    ) : SocketDtoMessage

    data class Unsubscribe(
        override val id: String,
        val data: TickerData,
    ) : SocketDtoMessage

    data class SymbolPriceChange(
        override val id: String,
        val data: SymbolPriceChangeData,
    ) : SocketDtoMessage

    data class Error(
        override val id: String,
        val data: ErrorData,
    ) : SocketDtoMessage
}
