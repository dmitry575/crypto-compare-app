package com.cryptocompare.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.entity.CatalogRemoteKeyEntity
import com.cryptocompare.data.mapper.normalizeSymbols
import com.cryptocompare.data.mapper.toAppException
import com.cryptocompare.data.mapper.toEntityFromDto
import com.cryptocompare.data.util.checkApiResponse
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
                    sortBy = CryptoCompareRepositoryConstants.CATALOG_SORT_BY,
                    sortDir = CryptoCompareRepositoryConstants.CATALOG_SORT_DIR,
                )

            checkApiResponse(response.errorCode, response.errorMsgs)

            // Листаем по тому, что прислал бэкенд, а не по тому, что осталось после
            // отсева строк без цены: иначе следующая страница начиналась бы раньше,
            // а страница, где отсеялось всё, выглядела бы концом каталога.
            val page = response.symbols.orEmpty()
            val symbols = page.normalizeSymbols()
            val endReached = page.isEmpty()

            database.withTransaction {
                // Пустой, но успешный ответ на REFRESH не должен обнулять каталог:
                // это почти наверняка временный сбой бэкенда, а не «символов больше
                // нет». Не трогаем ничего — прежние данные и ключ остаются, а
                // следующий refresh перестроит каталог, когда данные вернутся.
                // (Для APPEND пустой ответ — легитимный конец страниц, его пропускаем
                // ниже, чтобы записать endReached в ключ.)
                if (loadType == LoadType.REFRESH && page.isEmpty()) {
                    return@withTransaction
                }
                if (loadType == LoadType.REFRESH) {
                    symbolDao.deleteAll()
                }
                symbolDao.upsertAll(symbols.toEntityFromDto(syncedAtMillis))
                remoteKeyDao.upsert(
                    CatalogRemoteKeyEntity(
                        nextSkip = skip + page.size,
                        endReached = endReached,
                    ),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // экран каталога показывает эту ошибку сам — пусть она будет уже разобранной
            MediatorResult.Error(e.toAppException())
        }
    }
}
