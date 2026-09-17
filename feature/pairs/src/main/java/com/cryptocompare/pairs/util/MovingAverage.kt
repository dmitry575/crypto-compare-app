package com.cryptocompare.pairs.util

import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartIndicator

/**
 * Ряд скользящей средней по ценам закрытия — той же длины, что и свечи.
 *
 * `null` там, где истории не хватило: у SMA 50 это первые 49 свечей. Рисовать их
 * нулём или тянуть линию от края значило бы показать тренд, которого нет.
 *
 * EMA раскручивается от простой средней за первый период, а не от первой свечи:
 * старт от одной цены даёт заметный крюк в начале, который живёт десятки свечей.
 */
fun List<Candle>.movingAverage(indicator: ChartIndicator): List<Double?> {
    if (size < indicator.period) return List(size) { null }

    return if (indicator.exponential) {
        exponentialAverage(indicator.period)
    } else {
        simpleAverage(indicator.period)
    }
}

private fun List<Candle>.simpleAverage(period: Int): List<Double?> {
    var sum = 0.0

    return mapIndexed { index, candle ->
        sum += candle.close
        if (index >= period) sum -= this[index - period].close

        if (index >= period - 1) sum / period else null
    }
}

private fun List<Candle>.exponentialAverage(period: Int): List<Double?> {
    val smoothing = 2.0 / (period + 1)
    var previous = take(period).sumOf { it.close } / period

    return mapIndexed { index, candle ->
        when {
            index < period - 1 -> null
            index == period - 1 -> previous
            else -> {
                previous = candle.close * smoothing + previous * (1 - smoothing)
                previous
            }
        }
    }
}
