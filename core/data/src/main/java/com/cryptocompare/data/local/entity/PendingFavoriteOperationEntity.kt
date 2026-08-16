package com.cryptocompare.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "pending_favourite_operations",
    primaryKeys = ["userId", "ticker"],
)
data class PendingFavoriteOperationEntity(
    val userId: String,
    val ticker: String,
    val operation: Operation,
    val updatedAt: Long,
) {
    enum class Operation {
        ADD,
        DELETE,
    }
}
