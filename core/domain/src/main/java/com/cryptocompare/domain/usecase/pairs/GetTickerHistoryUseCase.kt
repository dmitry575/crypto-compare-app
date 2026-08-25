package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import javax.inject.Inject

/**
 * Одна страница истории свечей биржи. Свечи привязаны к провайдеру, а глубина
 * листается [offset]/[limit] — историю целиком в память не тянем.
 */
class GetTickerHistoryUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(
            providerId: Int,
            symbol: String,
            timeframe: ChartTimeframe,
            limit: Int,
            offset: Int,
        ): Result<List<Candle>> = cryptoCompareRepository.getCandles(providerId, symbol, timeframe, limit, offset)
    }
