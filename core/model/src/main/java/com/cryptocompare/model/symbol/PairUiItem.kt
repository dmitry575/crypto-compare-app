package com.cryptocompare.model.symbol

/** Пара в каталоге: цены сведены по всем биржам, где она торгуется. */
data class PairUiItem(
    val ticker: String,
    val symbolIds: List<Long>,
    val providerIds: List<Int>,
    val minPrice: Double,
    val maxPrice: Double,
    /**
     * Насколько цена расходится между биржами, в процентах от минимальной.
     * Ради этого числа приложение и существует, поэтому оно приходит готовым
     * из запроса, а не считается в слое представления.
     */
    val spreadPercent: Double,
) {
    /** Сколько бирж дают эту пару — подсказка, насколько разбросу можно верить. */
    val exchangeCount: Int get() = providerIds.size
}
