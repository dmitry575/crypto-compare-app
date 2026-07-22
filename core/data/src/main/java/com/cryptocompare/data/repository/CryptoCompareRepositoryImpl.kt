package com.cryptocompare.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.data.mapper.errorMessageOrNull
import com.cryptocompare.data.mapper.symbolToDomainFromDto
import com.cryptocompare.data.mapper.toCandles
import com.cryptocompare.data.mapper.toDomainFromEntity
import com.cryptocompare.data.mapper.toEntityFromDto
import com.cryptocompare.data.mapper.toPairUiItem
import com.cryptocompare.data.mapper.toRequestSpec
import com.cryptocompare.data.paging.SymbolsRemoteMediator
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.parseTicker
import com.cryptocompare.helpers.util.CryptoCompareRepositoryConstants
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.chart.HistoryResolution
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.symbol.Symbol
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.network.api.CryptoCompareApi
import com.cryptocompare.network.api.CryptoCompareHistoryApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.collections.orEmpty

@Singleton
class CryptoCompareRepositoryImpl
    @Inject
    constructor(
        private val cryptoCompareApi: CryptoCompareApi,
        private val historyApi: CryptoCompareHistoryApi,
        private val database: CryptoCompareDatabase,
        private val symbolDao: SymbolDao,
        private val providerDao: ProviderDao,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : CryptoCompareRepository {
        // ─── Providers ────────────────────────────────────────────────────────────

        // get providers
        override suspend fun getProviders(): Result<List<Provider>> =
            withContext(ioDispatcher) {
                runCatching {
                    val providers = providerDao.getAll().toDomainFromEntity()

                    if (providers.isNotEmpty() && !isCacheStale(providerDao.getLastUpdate())) {
                        return@runCatching providers
                    }
                    refreshProviders()
                }.onFailure { error -> if (error is CancellationException) throw error }
                    .recoverCatching { error ->
                        val cached = providerDao.getAll().toDomainFromEntity()
                        cached.ifEmpty { throw error }
                    }
            }

        // update providers
        private suspend fun refreshProviders(): List<Provider> {
            val syncedAtMillis = System.currentTimeMillis()
            val response = cryptoCompareApi.getProviders()

            if (response.errorCode != 0) {
                val message = response.errorMsgs?.joinToString("\n") ?: "Unknown error"
                throw IllegalStateException(message)
            }

            val providers = response.providers.orEmpty().toEntityFromDto(syncedAtMillis)
            providerDao.syncProviders(providers)
            return providers.toDomainFromEntity()
        }

        // ─── Symbols ──────────────────────────────────────────────────────────────

        // paged pairs: local Room cache is the source of truth, RemoteMediator
        // pulls catalog pages from the API on demand
        @OptIn(androidx.paging.ExperimentalPagingApi::class)
        override fun getPairsPaged(
            query: String,
            onlyFavourite: Boolean,
            favouriteTickers: Set<String>,
        ): Flow<PagingData<PairUiItem>> {
            val normalizedQuery = query.trim()
            val normalizedFavourites = favouriteTickers.map { it.trim().uppercase() }

            return Pager(
                config =
                    PagingConfig(
                        pageSize = CryptoCompareRepositoryConstants.SYMBOLS_IN_ROW,
                        enablePlaceholders = false,
                    ),
                remoteMediator =
                    SymbolsRemoteMediator(
                        api = cryptoCompareApi,
                        database = database,
                        refreshProviders = { refreshProviders() },
                    ),
                pagingSourceFactory = {
                    symbolDao.pagingPairs(
                        query = normalizedQuery,
                        onlyFavourite = onlyFavourite,
                        favouriteTickers = normalizedFavourites,
                    )
                },
            ).flow.map { pagingData -> pagingData.map { it.toPairUiItem() } }
        }

        // per-exchange rows live only on the network: the catalog collapses a
        // ticker to one row, so the API is the source of truth here and the
        // local cache is just an offline fallback
        override suspend fun getSymbolsByTicker(ticker: String): Result<List<Symbol>> =
            withContext(ioDispatcher) {
                runCatching {
                    val response = cryptoCompareApi.getSymbolsByTicker(ticker)

                    if (response.errorCode != 0) {
                        val message = response.errorMsgs?.joinToString("\n") ?: "Unknown error"
                        throw IllegalStateException(message)
                    }

                    response.symbols.orEmpty().symbolToDomainFromDto()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                }.recoverCatching { error ->
                    val cached = symbolDao.getByTicker(ticker).toDomainFromEntity()
                    cached.ifEmpty { throw error }
                }
            }

        // история свечей из стороннего min-api: ровно один запрос на масштаб,
        // без разбивки по биржам и без локального кеша
        override suspend fun getCandles(
            ticker: String,
            timeframe: ChartTimeframe,
        ): Result<List<Candle>> =
            withContext(ioDispatcher) {
                runCatching {
                    val (fromSymbol, toSymbol) =
                        ticker.parseTicker()
                            ?: throw IllegalStateException("Unsupported ticker format: $ticker")

                    val spec = timeframe.toRequestSpec()
                    val limit = CryptoCompareRepositoryConstants.CANDLE_LIMIT

                    val response =
                        when (spec.resolution) {
                            HistoryResolution.MINUTE ->
                                historyApi.getMinuteHistory(fromSymbol, toSymbol, limit, spec.aggregate)

                            HistoryResolution.HOUR ->
                                historyApi.getHourHistory(fromSymbol, toSymbol, limit, spec.aggregate)

                            HistoryResolution.DAY ->
                                historyApi.getDailyHistory(fromSymbol, toSymbol, limit, spec.aggregate)
                        }

                    response.errorMessageOrNull()?.let { throw IllegalStateException(it) }
                    response.toCandles()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                }
            }

        override suspend fun applyPriceUpdates(updates: List<TickerPrice>): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching {
                    symbolDao.updatePrices(
                        updates.map { update ->
                            Triple(update.symbolId.toLong(), update.priceBuy, update.priceSell)
                        },
                    )
                }.onFailure { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                }
            }

        override suspend fun refreshCatalog(): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching {
                    refreshSymbols()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                }
            }

        // full catalog sync, used by the background refresh worker
        private suspend fun refreshSymbols() {
            var skip = 0
            val syncedAtMillis = System.currentTimeMillis()
            val refreshedSymbols = mutableListOf<SymbolEntity>()
            refreshProviders()

            while (true) {
                val response =
                    cryptoCompareApi.getSymbols(
                        skip = skip,
                        rows = CryptoCompareRepositoryConstants.SYMBOLS_IN_ROW,
                    )

                if (response.errorCode != 0) {
                    val message = response.errorMsgs?.joinToString("\n") ?: "Unknown error"
                    throw IllegalStateException(message)
                }

                val symbols =
                    response.symbols
                        ?.map { symbol ->
                            symbol.copy(providerId = if (symbol.providerId == 0) 1 else symbol.providerId)
                        }.orEmpty()
                if (symbols.isEmpty()) break

                refreshedSymbols += symbols.toEntityFromDto(syncedAtMillis)
                skip += CryptoCompareRepositoryConstants.SYMBOLS_IN_ROW
            }

            symbolDao.syncSymbols(refreshedSymbols)
        }

        private fun isCacheStale(lastUpdatedMillis: Long): Boolean =
            System.currentTimeMillis() - lastUpdatedMillis >=
                CryptoCompareRepositoryConstants.CATALOG_CACHE_TTL_MILLIS
    }
