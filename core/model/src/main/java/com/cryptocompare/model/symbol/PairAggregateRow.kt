package com.cryptocompare.model.symbol

data class PairAggregateRow(
    val ticker: String,
    val symbolIds: String?,
    val providerIds: String?,
    val minPrice: Double,
    val maxPrice: Double,
)
