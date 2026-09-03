package com.cryptocompare.model.provider

data class ProviderDetail(
    val provider: Provider,
    val priceSell: Double?,
    val priceBuy: Double?,
    /** Объём за 24ч на этой бирже в базовом активе: BTC для BTC/USDT. */
    val volume24h: Double? = null,
    /** Объём за 24ч на этой бирже в котируемом активе: USDT для BTC/USDT. */
    val quoteVolume24h: Double? = null,
    /** Изменение цены за 24ч на этой бирже, в процентах. */
    val change24h: Double? = null,
)
