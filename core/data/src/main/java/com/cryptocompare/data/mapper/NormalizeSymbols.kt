package com.cryptocompare.data.mapper

import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolBestPriceDto

/**
 * Отбрасывает строки каталога без цены: без любой из двух сторон пара
 * бесполезна, а спред по ней не считается.
 *
 * Подстановки `providerId = 1` здесь больше нет. Она существовала ради внешнего
 * ключа на справочник бирж и подписывала **каждую** строку каталога первой
 * биржей подряд, независимо от того, откуда пришла цена. Ключа больше нет,
 * а у строки теперь две настоящие биржи — своя на каждую сторону.
 */
fun List<SymbolBestPriceDto>?.normalizeSymbols(): List<SymbolBestPriceDto> =
    this
        ?.filter { symbol ->
            symbol.bestAskPrice > 0 && symbol.bestBidPrice > 0
        }.orEmpty()
