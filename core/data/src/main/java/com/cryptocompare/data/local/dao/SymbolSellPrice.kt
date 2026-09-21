package com.cryptocompare.data.local.dao

/**
 * Цена продажи одного символа — проекция строки каталога для портфеля.
 *
 * Целая `SymbolEntity` здесь не нужна: из двух десятков полей портфель берёт
 * одно, а запрос по нему ещё и перечитывается на каждом сбросе тиков.
 */
data class SymbolSellPrice(
    val symbolId: Long,
    val sellPrice: Double,
)
