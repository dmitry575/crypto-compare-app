package com.cryptocompare.model.settings

import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe

/**
 * С чего открывается пара: с какой биржи и в каком масштабе графика.
 *
 * Тот, кто торгует на одной площадке, открывает каждую пару и первым делом
 * переключает биржу и масштаб — это настройка вместо двух касаний на каждой паре.
 */
data class MarketPreferences(
    /** Биржа по умолчанию. `null` — первая доступная у самой пары, как было. */
    val defaultProviderId: Int? = null,
    val timeframe: ChartTimeframe = ChartTimeframe.DEFAULT,
    /** Скользящие средние поверх свечей. Пусто — чистый график, как было. */
    val indicators: Set<ChartIndicator> = emptySet(),
)
