package com.cryptocompare.data.local.dao

/**
 * Одна строка пакетной записи цен из сокета.
 *
 * Раньше на этом месте был `Triple<Long, Double, Double>`: три безымянных
 * значения, где перепутать местами цену покупки и продажи стоило одного
 * неверного порядка аргументов и ничем не выдавало себя.
 */
data class SymbolBestPriceUpdate(
    val id: Long,
    val bestAskPrice: Double,
    val bestAskProviderId: Int?,
    val bestBidPrice: Double,
    val bestBidProviderId: Int?,
    val spreadPercent: Double?,
)
