package com.cryptocompare.model.chart

/** Какой ряд и с какой агрегацией запрашивать для выбранного масштаба. */
data class HistoryRequestSpec(
    val resolution: HistoryResolution,
    val aggregate: Int,
)
