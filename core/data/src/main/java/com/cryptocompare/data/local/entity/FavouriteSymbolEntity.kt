package com.cryptocompare.data.local.entity

import androidx.room.Entity

/**
 * Избранное хранится по символу, а не по тикеру: USDC в Ethereum и USDC в Solana —
 * разные активы (решение 1 в `CLAUDE.md`), и звезда на одной строке не должна
 * зажигаться на другой.
 *
 * [ticker] лежит рядом денормализованным: по нему понятно, что это за избранное,
 * когда символа в каталоге уже нет, и он же уходит в документ Firestore.
 */
@Entity(
    tableName = "favourite_symbols",
    primaryKeys = ["userId", "symbolId"],
)
data class FavouriteSymbolEntity(
    val userId: String,
    val symbolId: Long,
    val ticker: String,
    val updatedAt: Long,
)
