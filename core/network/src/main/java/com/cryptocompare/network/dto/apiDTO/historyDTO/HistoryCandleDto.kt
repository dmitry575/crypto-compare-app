package com.cryptocompare.network.dto.apiDTO.historyDTO

import com.google.gson.annotations.SerializedName

/** Одна свеча: время приходит в секундах epoch. */
data class HistoryCandleDto(
    @SerializedName("time") val time: Long?,
    @SerializedName("open") val open: Double?,
    @SerializedName("high") val high: Double?,
    @SerializedName("low") val low: Double?,
    @SerializedName("close") val close: Double?,
)
