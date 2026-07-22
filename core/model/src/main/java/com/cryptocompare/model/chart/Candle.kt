package com.cryptocompare.model.chart

data class Candle(
    val timeMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)
