package com.cryptocompare.helpers.util

/** Общие константы приложения, не привязанные к конкретному слою. */
object AppConstants {
    const val SPLASH_DURATION_MS = 2000L

    /** Котируемые валюты для разбора тикера на базу и котировку. */
    val QUOTE_CURRENCIES =
        listOf(
            "USDT",
            "USDC",
            "USDG",
            "USDF",
            "USD1",
            "USDE",
            "BUSD",
            "TUSD",
            "USDP",
            "FDUSD",
            "PYUSD",
            "DAI",
            "USD",
            "EUR",
            "GBP",
            "JPY",
            "AUD",
            "CAD",
            "BTC",
            "ETH",
            "BNB",
            "SOL",
            "XRP",
            "ADA",
            "DOT",
            "AVAX",
            "MATIC",
        )
}
