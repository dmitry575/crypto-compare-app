package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "symbols",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["providerId"])],
)
data class SymbolEntity(
    @PrimaryKey
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
    val updatedAt: String,
    val syncedAtMillis: Long,
    val volume24h: Double? = null,
    val quoteVolume24h: Double? = null,
    val change24h: Double? = null,
)
