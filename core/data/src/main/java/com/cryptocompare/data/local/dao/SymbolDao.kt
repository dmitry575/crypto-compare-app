package com.cryptocompare.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.model.symbol.PairAggregateRow

@Dao
interface SymbolDao {
    @Query("SELECT * FROM symbols ORDER BY id ASC")
    suspend fun getAll(): List<SymbolEntity>

    @Query("SELECT * FROM symbols WHERE ticker=:ticker COLLATE NOCASE")
    suspend fun getByTicker(ticker: String): List<SymbolEntity>

    /**
     * Каталог: одна строка на тикер, свёрнутая по всем биржам.
     *
     * Сырой запрос, а не `@Query`, потому что Room не подставляет `ORDER BY`
     * параметром. Текст собирает [com.cryptocompare.data.local.query.PairsPagingQuery];
     * там же объяснено, почему это безопасно.
     *
     * `observedEntities` обязателен: без него Room не узнает, что таблица
     * изменилась, и список перестанет обновляться на тиках цен.
     */
    @RawQuery(observedEntities = [SymbolEntity::class])
    fun pagingPairs(query: SupportSQLiteQuery): PagingSource<Int, PairAggregateRow>

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
