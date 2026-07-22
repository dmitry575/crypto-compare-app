package com.cryptocompare.data.mapper

import com.cryptocompare.data.util.DataConstants.HistoryDepth
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.chart.HistoryRequestSpec
import com.cryptocompare.model.chart.HistoryResolution

fun ChartTimeframe.toRequestSpec(): HistoryRequestSpec =
    when (this) {
        ChartTimeframe.M15 -> HistoryRequestSpec(HistoryResolution.MINUTE, 15, HistoryDepth.M15)
        ChartTimeframe.H1 -> HistoryRequestSpec(HistoryResolution.HOUR, 1, HistoryDepth.H1)
        ChartTimeframe.H4 -> HistoryRequestSpec(HistoryResolution.HOUR, 4, HistoryDepth.H4)
        ChartTimeframe.D1 -> HistoryRequestSpec(HistoryResolution.DAY, 1, HistoryDepth.D1)
        ChartTimeframe.W1 -> HistoryRequestSpec(HistoryResolution.DAY, 7, HistoryDepth.W1)
    }
