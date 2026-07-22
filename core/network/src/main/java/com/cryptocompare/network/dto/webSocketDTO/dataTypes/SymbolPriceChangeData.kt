package com.cryptocompare.network.dto.webSocketDTO.dataTypes

data class SymbolPriceChangeData(
    val ticker: String,
    val symbolId: Int,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
)
