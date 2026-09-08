package com.cryptocompare.data.mapper

import com.cryptocompare.model.symbol.PairAggregateRow
import com.cryptocompare.model.symbol.PairUiItem

fun PairAggregateRow.toPairUiItem(): PairUiItem =
    PairUiItem(
        ticker = ticker,
        buyPrice = buyPrice,
        sellPrice = sellPrice,
        spreadPercent = spreadPercent,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
    )
