package com.cryptocompare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    val id: Int,
    val name: String?,
    val website: String?,
    val status: String,
    val syncedAtMillis: Long,
)
