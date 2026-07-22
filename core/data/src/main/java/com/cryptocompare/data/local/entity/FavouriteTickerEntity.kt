package com.cryptocompare.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "favourite_tickers",
    primaryKeys = ["userId", "ticker"],
)
data class FavouriteTickerEntity(
    val userId: String,
    val ticker: String,
    val updatedAt: Long,
)
