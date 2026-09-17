package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptocompare.data.local.entity.PendingFavouriteOperationEntity

@Dao
interface PendingFavouriteOperationDao {
    @Query("SELECT * FROM pending_favourite_operations WHERE userId = :userId")
    suspend fun getAllByUser(userId: String): List<PendingFavouriteOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pendingFavouriteOperationEntity: PendingFavouriteOperationEntity)

    @Query(
        "DELETE FROM pending_favourite_operations " +
            "WHERE userId = :userId AND symbolId = :symbolId " +
            "AND operation = :operation AND updatedAt = :updatedAt",
    )
    suspend fun delete(
        userId: String,
        symbolId: Long,
        operation: PendingFavouriteOperationEntity.Operation,
        updatedAt: Long,
    )

    @Query("DELETE FROM pending_favourite_operations WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}
