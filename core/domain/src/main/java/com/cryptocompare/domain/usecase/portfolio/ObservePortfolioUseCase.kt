package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePortfolioUseCase
    @Inject
    constructor(
        private val portfolioRepository: PortfolioRepository,
    ) {
        operator fun invoke(): Flow<List<PortfolioPosition>> = portfolioRepository.observePositions()
    }
