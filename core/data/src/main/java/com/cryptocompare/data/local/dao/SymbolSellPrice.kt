package com.cryptocompare.data.local.dao

/**
 * Цена продажи одного символа для портфеля — из строки каталога (лучший bid)
 * или из последней котировки биржи, за которой закреплена позиция.
 *
 * Целая `SymbolEntity` здесь не нужна: из двух десятков полей портфель берёт
 * одно, а запрос по нему ещё и перечитывается на каждом сбросе тиков.
 */
data class SymbolSellPrice(
    val symbolId: Long,
    val sellPrice: Double,
    /** Биржа, чья цена взята. */
    val providerId: Int?,
    /** Её имя из справочника; `null`, пока биржи там нет. */
    val providerName: String?,
)
