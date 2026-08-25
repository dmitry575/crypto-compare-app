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
    val candles: List<Candle> = emptyList(),
    /** Грузим первую страницу графика (провайдер/масштаб сменились). */
    val chartLoading: Boolean = false,
    /** Догружаем соседнюю страницу истории при прокрутке к краю окна. */
    val chartLoadingMore: Boolean = false,
    /** Есть ли что подгрузить глубже в историю (левый край окна). */
    val chartCanLoadOlder: Boolean = false,
    /** Свежий край окна подрезан — есть что догрузить в сторону настоящего (правый край). */
    val chartCanLoadNewer: Boolean = false,
    val timeframe: ChartTimeframe = ChartTimeframe.DEFAULT,
) {
    val selectedExchange: ProviderDetail?
        get() = exchanges.getOrNull(selectedExchangeIndex)
}
