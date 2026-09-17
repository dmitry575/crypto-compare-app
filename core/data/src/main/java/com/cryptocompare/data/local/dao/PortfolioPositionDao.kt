package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioPositionDao {
    @Query("SELECT * FROM portfolio_positions ORDER BY ticker ASC, symbolId ASC")
    fun observeAll(): Flow<List<PortfolioPositionEntity>>

    @Query("SELECT * FROM portfolio_positions WHERE symbolId = :symbolId")
    suspend fun getBySymbol(symbolId: Long): PortfolioPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PortfolioPositionEntity)

    @Query("DELETE FROM portfolio_positions WHERE symbolId = :symbolId")
    suspend fun delete(symbolId: Long)
}
