package com.cryptocompare.pairs.navigation

import com.cryptocompare.pairs.util.PairsConstants
import java.net.URLEncoder

sealed class PairsScreens(
    val route: String,
) {
    object MainScreen : PairsScreens("main")

    object DetailsScreen : PairsScreens("details?ticker={ticker}&symbolId={symbolId}") {
        fun createRoute(
            ticker: String,
            symbolId: Long?,
        ): String = "details?ticker=${URLEncoder.encode(ticker, "UTF-8")}&symbolId=${symbolId.orNone()}"
    }

    object ComparisonScreen : PairsScreens("comparison?ticker={ticker}&symbolId={symbolId}") {
        fun createRoute(
            ticker: String,
            symbolId: Long?,
        ): String = "comparison?ticker=${URLEncoder.encode(ticker, "UTF-8")}&symbolId=${symbolId.orNone()}"
    }
}

private fun Long?.orNone(): Long = this ?: PairsConstants.Navigation.NO_SYMBOL_ID
