package com.cryptocompare.pairs.navigation

import java.net.URLEncoder

sealed class PairsScreens(
    val route: String,
) {
    object MainScreen : PairsScreens("main")

    object DetailsScreen : PairsScreens("details?ticker={ticker}") {
        fun createRoute(ticker: String): String = "details?ticker=${URLEncoder.encode(ticker, "UTF-8")}"
    }
}
