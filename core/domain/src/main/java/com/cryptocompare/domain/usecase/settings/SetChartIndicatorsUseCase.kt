package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.model.chart.ChartIndicator
import javax.inject.Inject

class SetChartIndicatorsUseCase
    @Inject
    constructor(
        private val marketPreferencesRepository: MarketPreferencesRepository,
    ) {
        /** Набор запоминается: включать средние на каждой паре заново — работа вместо инструмента. */
        suspend operator fun invoke(indicators: Set<ChartIndicator>) =
            marketPreferencesRepository.setChartIndicators(indicators)
    }
