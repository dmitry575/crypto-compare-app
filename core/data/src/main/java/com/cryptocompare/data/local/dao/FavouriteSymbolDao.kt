package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cryptocompare.data.local.entity.FavouriteSymbolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteSymbolDao {
    @Query("SELECT * FROM favourite_symbols WHERE userId = :userId ORDER by ticker ASC, symbolId ASC")
    fun observeUserSymbols(userId: String): Flow<List<FavouriteSymbolEntity>>

    @Query("SELECT * FROM favourite_symbols WHERE userId = :userId ORDER by ticker ASC, symbolId ASC")
    suspend fun getUserSymbols(userId: String): List<FavouriteSymbolEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_symbols WHERE userId = :userId AND symbolId = :symbolId)")
    suspend fun exists(
        userId: String,
        symbolId: Long,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteSymbolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FavouriteSymbolEntity>)

    @Query("DELETE FROM favourite_symbols WHERE userId = :userId AND symbolId = :symbolId")
    suspend fun delete(
        userId: String,
        symbolId: Long,
    )

    @Query("DELETE FROM favourite_symbols WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Transaction
    suspend fun replaceAll(
        userId: String,
        entities: List<FavouriteSymbolEntity>,
    ) {
        deleteByUser(userId)
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}
