package com.cryptocompare.model.symbol

data class PairUiItem(
    val ticker: String,
    val symbolIds: List<Long>,
    val providerIds: List<Int>,
    val minPrice: Double,
    val maxPrice: Double,
)
