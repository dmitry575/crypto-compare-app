package com.cryptocompare.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Избранное — по символу, а не по тикеру: у пары столько символов, сколько у неё
 * наборов сетей, и это разные активы.
 */
interface FavouriteSymbolRepository {
    fun observeFavouriteSymbolIds(): Flow<Set<Long>>

    /** [ticker] сохраняется рядом с id — им избранное называется, когда символа уже нет. */
    suspend fun toggleFavouriteSymbol(
        symbolId: Long,
        ticker: String,
    ): Result<Boolean>

    suspend fun syncFavouriteSymbols(): Result<Unit>

    suspend fun deleteAllFavourites(): Result<Unit>
}
