package com.cryptocompare.domain.repository

import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.settings.MarketPreferences
import kotlinx.coroutines.flow.Flow

interface MarketPreferencesRepository {
    fun observeMarketPreferences(): Flow<MarketPreferences>

    /** `null` — вернуться к «первой доступной бирже». */
    suspend fun setDefaultProvider(providerId: Int?)

    suspend fun setDefaultTimeframe(timeframe: ChartTimeframe)

    suspend fun setChartIndicators(indicators: Set<ChartIndicator>)
}
