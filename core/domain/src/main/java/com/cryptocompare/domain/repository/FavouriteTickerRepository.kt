package com.cryptocompare.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavouriteTickerRepository {
    fun observeFavouriteTickers(): Flow<Set<String>>

    suspend fun toggleFavouriteTicker(ticker: String): Result<Boolean>

    suspend fun syncFavouriteTickers(): Result<Unit>

    suspend fun deleteAllFavorites(): Result<Unit>
}
