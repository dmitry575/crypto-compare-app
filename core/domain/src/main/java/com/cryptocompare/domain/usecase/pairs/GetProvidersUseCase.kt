package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.model.provider.Provider
import javax.inject.Inject

class GetProvidersUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        /** Справочник бирж: репозиторий отдаёт его из Room и обновляет по сроку жизни кеша. */
        suspend operator fun invoke(): Result<List<Provider>> = cryptoCompareRepository.getProviders()
    }
