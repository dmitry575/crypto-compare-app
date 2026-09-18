package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.portfolio.PortfolioPosition
import javax.inject.Inject

class GetPortfolioPositionUseCase
    @Inject
    constructor(
        private val portfolioRepository: PortfolioRepository,
    ) {
        /** Форма правки открывается уже заполненной: позиция на символ одна. */
        suspend operator fun invoke(symbolId: Long): PortfolioPosition? = portfolioRepository.getPosition(symbolId)
    }
