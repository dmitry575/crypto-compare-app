package com.cryptocompare.model.chart

/** Какой ряд, с какой агрегацией и на какую глубину запрашивать для выбранного масштаба. */
data class HistoryRequestSpec(
    val resolution: HistoryResolution,
    val aggregate: Int,
    val limit: Int,
)
