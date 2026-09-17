package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Позиция портфеля. Хранится на устройстве и с аккаунтом не связана: портфель —
 * не избранное, синхронизировать его молча через Firestore мы не будем.
 */
@Entity(tableName = "portfolio_positions")
data class PortfolioPositionEntity(
    @PrimaryKey
    val symbolId: Long,
    val ticker: String,
    val amount: Double,
    val buyPrice: Double,
    val updatedAtMillis: Long,
)
