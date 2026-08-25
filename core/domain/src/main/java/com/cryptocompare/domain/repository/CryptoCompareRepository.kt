package com.cryptocompare.domain.repository

import androidx.paging.PagingData
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.symbol.Symbol
import com.cryptocompare.model.ticker.TickerPrice
import kotlinx.coroutines.flow.Flow

interface CryptoCompareRepository {
    suspend fun getProviders(): Result<List<Provider>>

    fun getPairsPaged(
        query: String,
        onlyFavourite: Boolean,
        favouriteTickers: Set<String>,
    ): Flow<PagingData<PairUiItem>>

    suspend fun getSymbolsByTicker(ticker: String): Result<List<Symbol>>

    /**
     * Страница истории свечей одной пары у конкретной биржи. Кэша нет — истории
     * слишком много; окно листается через [offset]/[limit]. Пустой список = у
     * биржи для этого окна свечей нет (в т.ч. монета там не торговалась).
     */
    suspend fun getCandles(
        providerId: Int,
        symbol: String,
        timeframe: ChartTimeframe,
        limit: Int,
        offset: Int,
    ): Result<List<Candle>>

    suspend fun applyPriceUpdates(updates: List<TickerPrice>): Result<Unit>

    suspend fun refreshCatalog(): Result<Unit>
}
