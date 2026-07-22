package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import javax.inject.Inject

class RefreshCatalogUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> = cryptoCompareRepository.refreshCatalog()
    }
