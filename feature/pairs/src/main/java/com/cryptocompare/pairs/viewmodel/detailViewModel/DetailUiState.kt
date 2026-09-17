package com.cryptocompare.pairs.viewmodel.detailViewModel

import com.cryptocompare.helpers.widest
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerBestPrice

data class DetailUiState(
    val ticker: String = "",
    /** Символ пары — один набор сетей. `null` — открыт тикер целиком. */
    val symbolId: Long? = null,
    /** Сети символа в едином виде. */
    val networks: List<String> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val exchanges: List<ProviderDetail> = emptyList(),
    val selectedExchangeIndex: Int = 0,
    /** Загруженная история графика, от старых свечей к новым. */
    val candles: List<Candle> = emptyList(),
    /**
     * Сколько баров в хвосте [candles] дорисовал живой тик поверх серверных.
     * По ним график считает абсолютный индекс свечи, который не съезжает при
     * догрузке истории.
     */
    val liveCount: Int = 0,
    /** Грузим первую страницу графика (провайдер/масштаб сменились). */
    val chartLoading: Boolean = false,
    /** Догружаем более старую страницу истории. */
    val chartLoadingOlder: Boolean = false,
    /** Есть ли что грузить глубже в историю. */
    val chartCanLoadOlder: Boolean = false,
    val timeframe: ChartTimeframe = ChartTimeframe.DEFAULT,
    /** Включённые скользящие средние; набор общий для всех пар и переживает выход. */
    val indicators: Set<ChartIndicator> = emptySet(),
    /** Лучшие пары бэкенда по символам тикера — из них берётся блок разницы. */
    val bestPrices: List<TickerBestPrice> = emptyList(),
) {
    val selectedExchange: ProviderDetail?
        get() = exchanges.getOrNull(selectedExchangeIndex)

    /** Самая широкая лучшая пара: то же число, что в каталоге и на экране сравнения. */
    val bestPair: TickerBestPrice?
        get() = bestPrices.widest()
}
