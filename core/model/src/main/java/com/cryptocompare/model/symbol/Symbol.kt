package com.cryptocompare.model.symbol

data class Symbol(
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
    /** Объём за 24ч в базовом активе: BTC для BTC/USDT. Между парами не сравним. */
    val volume24h: Double? = null,
    /** Объём за 24ч в котируемом активе: USDT для BTC/USDT. Сравним между парами. */
    val quoteVolume24h: Double? = null,
    /** Изменение цены за 24ч в процентах: 2.35 = +2.35 %. */
    val change24h: Double? = null,
)
