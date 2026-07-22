package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

data class GetSymbolsResponse(
    val errorCode: Int,
    val errorMsgs: List<String>?,
    val symbols: List<SymbolDto>?,
)
