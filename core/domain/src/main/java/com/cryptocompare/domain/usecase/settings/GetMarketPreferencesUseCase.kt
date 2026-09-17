package com.cryptocompare.domain.usecase.settings

import com.cryptocompare.domain.repository.MarketPreferencesRepository
import com.cryptocompare.model.settings.MarketPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetMarketPreferencesUseCase
    @Inject
    constructor(
        private val marketPreferencesRepository: MarketPreferencesRepository,
    ) {
        /**
         * Разовое чтение на открытии пары: настройка применяется к экрану один раз,
         * а не переставляет биржу под пальцем, если её поменять в профиле.
         */
        suspend operator fun invoke(): MarketPreferences =
            marketPreferencesRepository.observeMarketPreferences().first()
    }
