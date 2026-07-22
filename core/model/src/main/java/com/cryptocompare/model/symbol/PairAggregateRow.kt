package com.cryptocompare.model.symbol

/** Строка агрегата каталога: одна пара, свёрнутая по всем биржам. */
data class PairAggregateRow(
    val ticker: String,
    val symbolIds: String?,
    val providerIds: String?,
    val minPrice: Double,
    val maxPrice: Double,
    /** Разброс между биржами в процентах от минимальной цены. Считается в SQL. */
    val spreadPercent: Double,
)
