package com.cryptocompare.data.mapper

import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto

fun List<SymbolDto>?.normalizeSymbols(): List<SymbolDto> =
    this
        ?.map { symbol ->
            symbol.copy(providerId = if (symbol.providerId == 0) 1 else symbol.providerId)
        }?.filter { symbol ->
            symbol.priceSell > 0 && symbol.priceBuy > 0
        }.orEmpty()
