package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cryptocompare.data.local.entity.FavouriteTickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteTickerDao {
    @Query("SELECT * FROM favourite_tickers WHERE userId = :userId ORDER by ticker ASC")
    fun observeUserTickers(userId: String): Flow<List<FavouriteTickerEntity>>

    @Query("SELECT * FROM favourite_tickers WHERE userId = :userId ORDER by ticker ASC")
    suspend fun getUserTickers(userId: String): List<FavouriteTickerEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite_tickers WHERE userId = :userId AND ticker = :ticker)")
    suspend fun exists(
        userId: String,
        ticker: String,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteTickerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FavouriteTickerEntity>)

    @Query("DELETE FROM favourite_tickers WHERE userId = :userId AND ticker = :ticker")
    suspend fun delete(
        userId: String,
        ticker: String,
    )

    @Query("DELETE FROM favourite_tickers WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Transaction
    suspend fun replaceAll(
        userId: String,
        entities: List<FavouriteTickerEntity>,
    ) {
        deleteByUser(userId)
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
    }
}
