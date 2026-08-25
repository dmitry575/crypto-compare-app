package com.cryptocompare.network.dto.apiDTO.klinesDTO

/**
 * Ответ нашего бэкенда на `GET /v1/klines/{providerId}`: свечи одной пары у
 * конкретной биржи. В отличие от прежнего стороннего min-api, ряд привязан к
 * провайдеру, а окно задаётся `limit`/`offset` — историю листаем страницами.
 */
data class GetKlinesResponse(
    val errorCode: Int,
    val errorMsgs: List<String>?,
    val providerId: Int?,
    val providerName: String?,
    val ticker: String?,
    val interval: String?,
    val klines: List<KlineEntryDto>?,
)
