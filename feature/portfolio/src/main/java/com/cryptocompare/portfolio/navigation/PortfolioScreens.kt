package com.cryptocompare.portfolio.navigation

import android.net.Uri
import com.cryptocompare.portfolio.util.PortfolioConstants

sealed class PortfolioScreens(
    val route: String,
) {
    object PortfolioScreen : PortfolioScreens("portfolio")

    object PositionEditScreen : PortfolioScreens(
        "portfolio/position?" +
            "${PortfolioConstants.Navigation.SYMBOL_ID_ARG}={${PortfolioConstants.Navigation.SYMBOL_ID_ARG}}&" +
            "${PortfolioConstants.Navigation.TICKER_ARG}={${PortfolioConstants.Navigation.TICKER_ARG}}&" +
            "${PortfolioConstants.Navigation.PRICE_ARG}={${PortfolioConstants.Navigation.PRICE_ARG}}&" +
            "${PortfolioConstants.Navigation.PROVIDER_ID_ARG}={${PortfolioConstants.Navigation.PROVIDER_ID_ARG}}",
    ) {
        /**
         * [price] и [providerId] — подсказка для новой позиции: ask и биржа,
         * открытые на экране пары. У существующей форма берёт её собственные
         * среднюю цену и биржу, а подсказку игнорирует.
         */
        fun createRoute(
            symbolId: Long,
            ticker: String,
            price: Double? = null,
            providerId: Int? = null,
        ): String =
            "portfolio/position?" +
                "${PortfolioConstants.Navigation.SYMBOL_ID_ARG}=$symbolId&" +
                "${PortfolioConstants.Navigation.TICKER_ARG}=${Uri.encode(ticker)}" +
                price?.let { "&${PortfolioConstants.Navigation.PRICE_ARG}=$it" }.orEmpty() +
                providerId?.let { "&${PortfolioConstants.Navigation.PROVIDER_ID_ARG}=$it" }.orEmpty()
    }
}
