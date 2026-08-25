package com.cryptocompare.data.mapper

import com.cryptocompare.data.util.DataConstants.Klines
import com.cryptocompare.model.chart.ChartTimeframe

/** Масштаб графика → строка интервала бэкенда (`1m`, `1h`, `1d`, …). */
fun ChartTimeframe.toKlineInterval(): String =
    when (this) {
        ChartTimeframe.M15 -> Klines.INTERVAL_M15
        ChartTimeframe.H1 -> Klines.INTERVAL_H1
        ChartTimeframe.H4 -> Klines.INTERVAL_H4
        ChartTimeframe.D1 -> Klines.INTERVAL_D1
        ChartTimeframe.W1 -> Klines.INTERVAL_W1
    }
