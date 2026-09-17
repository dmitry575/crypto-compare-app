package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.model.settings.MarketPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMarketPreferencesUseCase
    @Inject
    constructor(
        private val marketPreferencesRepository: MarketPreferencesRepository,
    ) {
        operator fun invoke(): Flow<MarketPreferences> = marketPreferencesRepository.observeMarketPreferences()
    }
