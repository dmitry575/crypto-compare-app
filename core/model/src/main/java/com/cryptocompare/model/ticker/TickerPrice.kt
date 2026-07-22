package com.cryptocompare.model.ticker

data class TickerPrice(
    val ticker: String,
    val symbolId: Int,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
)
