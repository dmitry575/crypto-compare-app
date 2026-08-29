package com.cryptocompare.model.symbol

data class Symbol(
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
    val volume24h: Double? = null,
    val change24h: Double? = null,
)
