package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import com.cryptocompare.model.portfolio.PortfolioHolding
import com.cryptocompare.model.portfolio.PortfolioSummary

data class PortfolioUiState(
    val holdings: List<PortfolioHolding> = emptyList(),
    /** `null` — считать итог не из чего: портфель пуст или цен нет ни по одной позиции. */
    val summary: PortfolioSummary? = null,
    /** Пока база не ответила, пустой список — это ещё не «портфель пуст». */
    val loading: Boolean = true,
)
