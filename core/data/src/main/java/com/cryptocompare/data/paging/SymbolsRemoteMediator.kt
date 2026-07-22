package com.cryptocompare.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.entity.CatalogRemoteKeyEntity
import com.cryptocompare.data.mapper.toEntityFromDto
import com.cryptocompare.helpers.util.CryptoCompareRepositoryConstants
import com.cryptocompare.model.symbol.PairAggregateRow
import com.cryptocompare.network.api.CryptoCompareApi
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
class SymbolsRemoteMediator(
    private val api: CryptoCompareApi,
    private val database: CryptoCompareDatabase,
    private val refreshProviders: suspend () -> Unit,
) : RemoteMediator<Int, PairAggregateRow>() {
    private val symbolDao = database.symbolDao()
    private val remoteKeyDao = database.catalogRemoteKeyDao()

    override suspend fun initialize(): InitializeAction {
        val lastUpdate = symbolDao.getLastUpdate()
        val isFresh =
            lastUpdate > 0L &&
                System.currentTimeMillis() - lastUpdate <
                CryptoCompareRepositoryConstants.CATALOG_CACHE_TTL_MILLIS

        return if (isFresh) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PairAggregateRow>,
    ): MediatorResult {
        val skip =
            when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val key = remoteKeyDao.get()
                    when {
                        key == null -> 0
                        key.endReached -> return MediatorResult.Success(endOfPaginationReached = true)
                        else -> key.nextSkip
                    }
                }
            }

        return try {
            if (loadType == LoadType.REFRESH) {
                refreshProviders()
            }

            val syncedAtMillis = System.currentTimeMillis()
            val response =
                api.getSymbols(
                    skip = skip,
                    rows = CryptoCompareRepositoryConstants.SYMBOLS_IN_ROW,
                )

            if (response.errorCode != 0) {
                val message = response.errorMsgs?.joinToString("\n") ?: "Unknown error"
                return MediatorResult.Error(IllegalStateException(message))
            }

            val symbols =
                response.symbols
                    ?.map { symbol ->
                        symbol.copy(providerId = if (symbol.providerId == 0) 1 else symbol.providerId)
                    }.orEmpty()
            val endReached = symbols.isEmpty()

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    symbolDao.deleteAll()
                }
                symbolDao.upsertAll(symbols.toEntityFromDto(syncedAtMillis))
                remoteKeyDao.upsert(
                    CatalogRemoteKeyEntity(
                        nextSkip = skip + symbols.size,
                        endReached = endReached,
                    ),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
