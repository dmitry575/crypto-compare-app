package com.cryptocompare.network.dto.apiDTO.historyDTO

import com.google.gson.annotations.SerializedName

/**
 * Ответ CryptoCompare min-api (data/v2/histoday и соседние):
 * { "Response": "Success", "Data": { "Data": [ { time, open, high, low, close }, ... ] } }
 */
data class GetHistoryResponse(
    @SerializedName("Response") val response: String?,
    @SerializedName("Message") val message: String?,
    @SerializedName("Data") val data: HistoryDataDto?,
    @SerializedName("Err") val error: HistoryErrorDto?,
)
