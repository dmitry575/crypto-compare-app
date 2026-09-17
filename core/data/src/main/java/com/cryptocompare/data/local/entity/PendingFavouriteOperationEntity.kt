package com.cryptocompare.data.local.entity

import androidx.room.Entity

/** Правка избранного, которая ещё не доехала до Firestore. */
@Entity(
    tableName = "pending_favourite_operations",
    primaryKeys = ["userId", "symbolId"],
)
data class PendingFavouriteOperationEntity(
    val userId: String,
    val symbolId: Long,
    val ticker: String,
    val operation: Operation,
    val updatedAt: Long,
) {
    enum class Operation {
        ADD,
        DELETE,
    }
}
