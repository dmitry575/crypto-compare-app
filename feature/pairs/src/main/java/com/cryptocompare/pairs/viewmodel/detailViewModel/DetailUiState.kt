package com.cryptocompare.pairs.viewmodel.detailViewModel

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.ProviderDetail

data class DetailUiState(
    val ticker: String = "",
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
) {
    val selectedExchange: ProviderDetail?
        get() = exchanges.getOrNull(selectedExchangeIndex)
}
