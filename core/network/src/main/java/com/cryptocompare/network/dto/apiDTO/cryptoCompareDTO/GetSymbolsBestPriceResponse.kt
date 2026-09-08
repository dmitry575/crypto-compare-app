package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

/** Ответ ленты каталога: строки с лучшей парой цен по каждому тикеру. */
data class GetSymbolsBestPriceResponse(
    val errorCode: Int,
    val errorMsgs: List<String>?,
    val symbols: List<SymbolBestPriceDto>?,
)
