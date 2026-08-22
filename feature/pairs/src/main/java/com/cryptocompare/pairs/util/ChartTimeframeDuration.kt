package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.util.PairsConstants.Chart

/** Длительность одной свечи масштаба в миллисекундах. */
internal fun ChartTimeframe.durationMillis(): Long =
    when (this) {
        ChartTimeframe.M15 -> Chart.M15_DURATION_MS
        ChartTimeframe.H1 -> Chart.H1_DURATION_MS
        ChartTimeframe.H4 -> Chart.H4_DURATION_MS
        ChartTimeframe.D1 -> Chart.D1_DURATION_MS
        ChartTimeframe.W1 -> Chart.W1_DURATION_MS
    }
