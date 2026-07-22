package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_remote_key")
data class CatalogRemoteKeyEntity(
    @PrimaryKey
    val id: Int = 0,
    val nextSkip: Int,
    val endReached: Boolean,
)
