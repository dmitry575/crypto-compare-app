package com.cryptocompare.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.model.symbol.PairAggregateRow

@Dao
interface SymbolDao {
    @Query("SELECT * FROM symbols ORDER BY id ASC")
    suspend fun getAll(): List<SymbolEntity>

    @Query("SELECT * FROM symbols WHERE ticker=:ticker COLLATE NOCASE")
    suspend fun getByTicker(ticker: String): List<SymbolEntity>

    @Query(
        """
        SELECT
            UPPER(ticker) AS ticker,
            GROUP_CONCAT(id) AS symbolIds,
            GROUP_CONCAT(providerId) AS providerIds,
            MIN(MIN(priceBuy, priceSell)) AS minPrice,
            MAX(MAX(priceBuy, priceSell)) AS maxPrice,
            CASE
                WHEN MIN(MIN(priceBuy, priceSell)) > 0
                THEN (MAX(MAX(priceBuy, priceSell)) - MIN(MIN(priceBuy, priceSell)))
                     * 100.0 / MIN(MIN(priceBuy, priceSell))
                ELSE 0
            END AS spreadPercent
        FROM symbols
        WHERE ticker IS NOT NULL AND TRIM(ticker) != ''
            AND (:query = '' OR ticker LIKE '%' || :query || '%')
            AND (:onlyFavourite = 0 OR UPPER(ticker) IN (:favouriteTickers))
        GROUP BY UPPER(ticker)
        ORDER BY UPPER(ticker) ASC
        """,
    )
    fun pagingPairs(
        query: String,
        onlyFavourite: Boolean,
        favouriteTickers: List<String>,
    ): PagingSource<Int, PairAggregateRow>

    @Query("UPDATE symbols SET priceBuy = :priceBuy, priceSell = :priceSell WHERE id = :id")
    suspend fun updatePrice(
        id: Long,
        priceBuy: Double,
        priceSell: Double,
    )

    @Transaction
    suspend fun updatePrices(updates: List<Triple<Long, Double, Double>>) {
        updates.forEach { (id, priceBuy, priceSell) ->
            updatePrice(id = id, priceBuy = priceBuy, priceSell = priceSell)
        }
    }

    @Query("DELETE FROM symbols")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(symbols: List<SymbolEntity>)

    @Query("DELETE FROM symbols WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<Long>)

    @Query("SELECT MAX(syncedAtMillis) FROM symbols")
    suspend fun getLastUpdate(): Long

    @Transaction
    suspend fun syncSymbols(symbols: List<SymbolEntity>) {
        if (symbols.isEmpty()) {
            deleteAll()
            return
        }

        upsertAll(symbols)
        deleteAllExcept(symbols.map(SymbolEntity::id))
    }
}
