package com.cryptocompare.app.navigation

import com.cryptocompare.pairs.navigation.PairsDestination
import com.cryptocompare.portfolio.navigation.PortfolioDestination

/** Вкладка нижней панели — корень графа своей фичи. */
internal enum class AppTab(
    val graphRoute: String,
) {
    PAIRS(PairsDestination.ROUTE),
    PORTFOLIO(PortfolioDestination.ROUTE),
}
