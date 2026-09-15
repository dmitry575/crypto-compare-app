package com.cryptocompare.data.mapper

import com.cryptocompare.helpers.networkNames
import com.cryptocompare.model.symbol.PairAggregateRow
import com.cryptocompare.model.symbol.PairUiItem

fun PairAggregateRow.toPairUiItem(): PairUiItem =
    PairUiItem(
        symbolId = symbolId,
        ticker = ticker,
        buyPrice = buyPrice,
        sellPrice = sellPrice,
        spreadPercent = spreadPercent,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
        networks = networkNames(network, baseAsset = symbol?.substringBefore(SYMBOL_DELIMITER)),
        networkCount = networkCount,
    )

private const val SYMBOL_DELIMITER = '/'
