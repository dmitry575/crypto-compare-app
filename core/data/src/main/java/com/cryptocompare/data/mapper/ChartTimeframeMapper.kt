package com.cryptocompare.data.mapper

import com.cryptocompare.data.model.HistoryRequestSpec
import com.cryptocompare.data.model.HistoryResolution
import com.cryptocompare.model.chart.ChartTimeframe

fun ChartTimeframe.toRequestSpec(): HistoryRequestSpec =
    when (this) {
        ChartTimeframe.M15 -> HistoryRequestSpec(HistoryResolution.MINUTE, 15)
        ChartTimeframe.H1 -> HistoryRequestSpec(HistoryResolution.HOUR, 1)
        ChartTimeframe.H4 -> HistoryRequestSpec(HistoryResolution.HOUR, 4)
        ChartTimeframe.D1 -> HistoryRequestSpec(HistoryResolution.DAY, 1)
        ChartTimeframe.W1 -> HistoryRequestSpec(HistoryResolution.DAY, 7)
    }
