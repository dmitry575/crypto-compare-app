package com.cryptocompare.network.dto.apiDTO.historyDTO

import com.google.gson.annotations.SerializedName

/** Тело ошибки min-api (например, отсутствующий API-ключ). */
data class HistoryErrorDto(
    @SerializedName("message") val message: String?,
)
