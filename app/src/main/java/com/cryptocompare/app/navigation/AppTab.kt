package com.cryptocompare.app.navigation

import com.cryptocompare.pairs.navigation.PairsRoute
import com.cryptocompare.portfolio.navigation.PortfolioRoute

/** Вкладка нижней панели — корень графа своей фичи. */
internal enum class AppTab(
    val graphRoute: Any,
) {
    PAIRS(PairsRoute.Graph),
    PORTFOLIO(PortfolioRoute.Graph),
}
