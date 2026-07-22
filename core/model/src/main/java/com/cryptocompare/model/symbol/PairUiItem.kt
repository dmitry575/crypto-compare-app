package com.cryptocompare.model.symbol

/** Пара в каталоге: одна строка на тикер, сведённая бэкендом по всем биржам. */
data class PairUiItem(
    val ticker: String,
    val symbolIds: List<Long>,
    val providerIds: List<Int>,
    val minPrice: Double,
    val maxPrice: Double,
    /**
     * Спред пары в процентах от цены покупки — насколько широк рынок.
     *
     * Это **не** разброс между биржами: каталог отдаёт одну строку на тикер
     * с общим providerId, и разбивка по биржам есть только на детальном экране,
     * который ходит в отдельный эндпоинт. Считается в SQL вместе с min/max.
     */
    val spreadPercent: Double,
)
