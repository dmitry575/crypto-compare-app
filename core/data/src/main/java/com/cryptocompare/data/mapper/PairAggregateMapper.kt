package com.cryptocompare.data.mapper

import com.cryptocompare.model.symbol.PairAggregateRow
import com.cryptocompare.model.symbol.PairUiItem

fun PairAggregateRow.toPairUiItem(): PairUiItem =
    PairUiItem(
        ticker = ticker,
        symbolIds = symbolIds?.split(",")?.mapNotNull(String::toLongOrNull).orEmpty(),
        providerIds =
            providerIds
                ?.split(",")
                ?.mapNotNull(String::toIntOrNull)
                ?.filter { it > 0 }
                ?.distinct()
                .orEmpty(),
        minPrice = minPrice,
        maxPrice = maxPrice,
    )
