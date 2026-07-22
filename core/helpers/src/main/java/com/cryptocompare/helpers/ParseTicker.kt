package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.AppConstants.QUOTE_CURRENCIES

fun String.parseTicker(): Pair<String, String>? {
    val separators = listOf("/", "-", "_")
    for (sep in separators) {
        if (contains(sep)) {
            val parts = split(sep)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return parts[0].uppercase() to parts[1].uppercase()
            }
        }
    }
    val upper = uppercase()
    for (quote in QUOTE_CURRENCIES) {
        if (upper.endsWith(quote) && upper.length > quote.length) {
            val base = upper.dropLast(quote.length)
            if (base.isNotBlank()) return base to quote
        }
    }
    return null
}
