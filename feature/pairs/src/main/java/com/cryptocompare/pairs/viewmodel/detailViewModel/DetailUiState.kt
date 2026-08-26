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
    /** Грузим историю графика (провайдер/масштаб сменились). */
    val chartLoading: Boolean = false,
    val timeframe: ChartTimeframe = ChartTimeframe.DEFAULT,
) {
    val selectedExchange: ProviderDetail?
        get() = exchanges.getOrNull(selectedExchangeIndex)
}
