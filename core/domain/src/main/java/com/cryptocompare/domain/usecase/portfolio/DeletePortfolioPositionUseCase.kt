package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.PortfolioRepository
import javax.inject.Inject

class DeletePortfolioPositionUseCase
    @Inject
    constructor(
        private val portfolioRepository: PortfolioRepository,
    ) {
        suspend operator fun invoke(symbolId: Long): Result<Unit> = portfolioRepository.deletePosition(symbolId)
    }
