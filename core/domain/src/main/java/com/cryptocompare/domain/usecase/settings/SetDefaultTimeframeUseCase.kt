package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.model.chart.ChartTimeframe
import javax.inject.Inject

class SetDefaultTimeframeUseCase
    @Inject
    constructor(
        private val marketPreferencesRepository: MarketPreferencesRepository,
    ) {
        suspend operator fun invoke(timeframe: ChartTimeframe) =
            marketPreferencesRepository.setDefaultTimeframe(timeframe)
    }
