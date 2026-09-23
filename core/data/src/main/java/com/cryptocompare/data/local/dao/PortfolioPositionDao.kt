package com.cryptocompare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import com.cryptocompare.data.local.entity.PortfolioQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioPositionDao {
    @Query(
        """
        SELECT pos.*, p.name AS exchangeName
        FROM portfolio_positions pos
        LEFT JOIN providers p ON p.id = pos.providerId
        ORDER BY pos.ticker ASC, pos.symbolId ASC
        """,
    )
    fun observeAll(): Flow<List<PortfolioPositionWithExchange>>

    @Query(
        """
        SELECT pos.*, p.name AS exchangeName
        FROM portfolio_positions pos
        LEFT JOIN providers p ON p.id = pos.providerId
        WHERE pos.symbolId = :symbolId
        """,
    )
    suspend fun getBySymbol(symbolId: Long): PortfolioPositionWithExchange?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PortfolioPositionEntity)

    @Query("DELETE FROM portfolio_positions WHERE symbolId = :symbolId")
    suspend fun deletePosition(symbolId: Long)

    @Query("DELETE FROM portfolio_quotes WHERE symbolId = :symbolId")
    suspend fun deleteQuote(symbolId: Long)

    /** Позиция уходит вместе со своей последней ценой: хранить её больше незачем. */
    @Transaction
    suspend fun delete(symbolId: Long) {
        deletePosition(symbolId)
        deleteQuote(symbolId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotes(quotes: List<PortfolioQuoteEntity>)

    /**
     * Последние цены закреплённых бирж — только те, что относятся к бирже
     * позиции сейчас. После смены биржи прежняя цена остаётся в таблице до
     * первой котировки новой, но в портфель уже не попадает: выдать цену одной
     * площадки за цену другой хуже, чем показать прочерк.
     */
    @Query(
        """
        SELECT q.symbolId AS symbolId,
               q.price AS sellPrice,
               q.providerId AS providerId,
               p.name AS providerName
        FROM portfolio_quotes q
        JOIN portfolio_positions pos ON pos.symbolId = q.symbolId AND pos.providerId = q.providerId
        LEFT JOIN providers p ON p.id = q.providerId
        """,
    )
    fun observePinnedQuotes(): Flow<List<SymbolSellPrice>>
}
