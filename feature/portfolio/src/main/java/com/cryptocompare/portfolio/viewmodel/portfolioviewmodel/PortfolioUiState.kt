package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import com.cryptocompare.model.portfolio.PortfolioPosition

data class PortfolioUiState(
    val positions: List<PortfolioPosition> = emptyList(),
    /** Пока база не ответила, пустой список — это ещё не «портфель пуст». */
    val loading: Boolean = true,
)
