package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Последний bid биржи, за которой закреплена позиция. Строка на позицию.
 *
 * Отдельной таблицей, а не столбцами позиции: форма сохраняет позицию целиком,
 * и правка количества стирала бы последнюю цену. Здесь же цена переживает
 * правку, а при смене биржи просто перестаёт подходить — чтение берёт её только
 * при совпадении [providerId] с биржей позиции.
 */
@Entity(tableName = "portfolio_quotes")
data class PortfolioQuoteEntity(
    @PrimaryKey
    val symbolId: Long,
    val providerId: Int,
    val price: Double,
    val quotedAtMillis: Long,
)
