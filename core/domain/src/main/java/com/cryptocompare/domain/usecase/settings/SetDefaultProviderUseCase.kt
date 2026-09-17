package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.MarketPreferencesRepository
import javax.inject.Inject

class SetDefaultProviderUseCase
    @Inject
    constructor(
        private val marketPreferencesRepository: MarketPreferencesRepository,
    ) {
        suspend operator fun invoke(providerId: Int?) = marketPreferencesRepository.setDefaultProvider(providerId)
    }
