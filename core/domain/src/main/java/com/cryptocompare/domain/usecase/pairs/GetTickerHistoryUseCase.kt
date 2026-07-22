package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import javax.inject.Inject

class GetTickerHistoryUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(
            ticker: String,
            timeframe: ChartTimeframe,
        ): Result<List<Candle>> = cryptoCompareRepository.getCandles(ticker, timeframe)
    }
