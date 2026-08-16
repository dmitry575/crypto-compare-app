package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptocompare.data.local.entity.PendingFavoriteOperationEntity

@Dao
interface PendingFavouriteOperationDao {
    @Query("SELECT * FROM pending_favourite_operations WHERE userId = :userId")
    suspend fun getAllByUser(userId: String): List<PendingFavoriteOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pendingFavoriteOperationEntity: PendingFavoriteOperationEntity)

    @Query(
        "DELETE FROM pending_favourite_operations " +
            "WHERE userId = :userId AND ticker = :ticker " +
            "AND operation = :operation AND updatedAt = :updatedAt",
    )
    suspend fun delete(
        userId: String,
        ticker: String,
        operation: PendingFavoriteOperationEntity.Operation,
        updatedAt: Long,
    )

    @Query("DELETE FROM pending_favourite_operations WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
