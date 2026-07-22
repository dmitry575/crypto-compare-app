package com.cryptocompare.network.dto.apiDTO.historyDTO

import com.google.gson.annotations.SerializedName

/** Обёртка min-api: полезные свечи лежат во вложенном поле "Data". */
data class HistoryDataDto(
    @SerializedName("Data") val entries: List<HistoryCandleDto>?,
)
