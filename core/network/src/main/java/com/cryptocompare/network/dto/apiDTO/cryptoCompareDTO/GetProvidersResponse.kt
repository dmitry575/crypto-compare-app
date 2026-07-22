package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

data class GetProvidersResponse(
    val errorCode: Int,
    val errorMsgs: List<String>?,
    val providers: List<ProviderDto>?,
)
