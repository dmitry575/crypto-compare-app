package com.cryptocompare.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolBestPriceUpdate
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.data.local.query.PairsPagingQuery
import com.cryptocompare.data.mapper.normalizeSymbols
import com.cryptocompare.data.mapper.symbolToDomainFromDto
import com.cryptocompare.data.mapper.toCandles
import com.cryptocompare.data.mapper.toDomainFromEntity
import com.cryptocompare.data.mapper.toEntityFromDto
import com.cryptocompare.data.mapper.toKlineInterval
import com.cryptocompare.data.mapper.toPairUiItem
import com.cryptocompare.data.mapper.toSellQuotes
import com.cryptocompare.data.mapper.toTickerBestPrice
import com.cryptocompare.data.paging.SymbolsRemoteMediator
import com.cryptocompare.data.util.appRunCatching
import com.cryptocompare.data.util.checkApiResponse
import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.helpers.util.CryptoCompareRepositoryConstants
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSorting
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.symbol.Symbol
import com.cryptocompare.model.symbol.SymbolSellQuote
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.network.api.CryptoCompareApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
        private val database: CryptoCompareDatabase,
        private val symbolDao: SymbolDao,
        private val providerDao: ProviderDao,
        @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : CryptoCompareRepository {
        // ─── Providers ────────────────────────────────────────────────────────────

        // get providers
        override suspend fun getProviders(): Result<List<Provider>> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val providers = providerDao.getAll().toDomainFromEntity()

                    if (providers.isNotEmpty() && !isCacheStale(providerDao.getLastUpdate())) {
                        return@appRunCatching providers
                    }
                    refreshProviders()
                }.recoverCatching { error ->
                    val cached = providerDao.getAll().toDomainFromEntity()
                    cached.ifEmpty { throw error }
                }
            }

        /**
         * Справочник бирж целиком, страницами.
         *
         * Без `rows` эндпоинт отдаёт десять штук, и приложение знало десять бирж
         * из тридцати одной. Всё, что не нашлось в справочнике, молча выпадало
         * с детального экрана — там биржа без провайдера отбрасывается.
         */
        private suspend fun refreshProviders(): List<Provider> {
            val syncedAtMillis = System.currentTimeMillis()
            val providers = mutableListOf<ProviderEntity>()
            var skip = 0

            while (true) {
                val response =
                    cryptoCompareApi.getProviders(
                        skip = skip,
                        rows = CryptoCompareRepositoryConstants.PROVIDERS_IN_ROW,
                    )

                checkApiResponse(response.errorCode, response.errorMsgs)

                val page = response.providers.orEmpty()
                if (page.isEmpty()) break

                providers += page.toEntityFromDto(syncedAtMillis)
                skip += page.size

                if (page.size < CryptoCompareRepositoryConstants.PROVIDERS_IN_ROW) break
            }

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
            favouriteSymbolIds: Set<Long>,
            direction: CatalogDirection,
            sorting: CatalogSorting,
        ): Flow<PagingData<PairUiItem>> {
            val normalizedQuery = query.trim()

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
                        PairsPagingQuery.build(
                            query = normalizedQuery,
                            onlyFavourite = onlyFavourite,
                            favouriteSymbolIds = favouriteSymbolIds.toList(),
                            direction = direction,
                            sorting = sorting,
                        ),
                    )
                },
            ).flow.map { pagingData ->
                pagingData.filter { it.buyPrice > 0 && it.sellPrice > 0 }.map { it.toPairUiItem() }
            }
        }

        /**
         * Котировки по биржам: только из сети.
         *
         * Оффлайн-подмены из локальной таблицы здесь больше нет. Каталог хранит
         * строку на тикер с лучшей парой цен, где стороны взяты с **разных**
         * бирж, — выдавать её за котировку одной биржи нельзя. Пока такая
         * подмена существовала, экран без сети показывал одну выдуманную биржу
         * с ценами, которых у неё нет.
         */
        override suspend fun getSymbolsByTicker(ticker: String): Result<List<Symbol>> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val response = cryptoCompareApi.getSymbolsByTicker(ticker)

                    checkApiResponse(response.errorCode, response.errorMsgs)

                    response.symbols.orEmpty().symbolToDomainFromDto()
                }
            }

        // история свечей с нашего бэкенда: одна страница на биржу, окно листается
        // offset/limit, локального кеша нет — истории слишком много
        override suspend fun getCandles(
            providerId: Int,
            symbol: String,
            timeframe: ChartTimeframe,
            limit: Int,
            offset: Int,
        ): Result<List<Candle>> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val response =
                        cryptoCompareApi.getKlines(
                            providerId = providerId,
                            symbol = symbol,
                            interval = timeframe.toKlineInterval(),
                            limit = limit,
                            offset = offset,
                        )

                    checkApiResponse(response.errorCode, response.errorMsgs)

                    response.toCandles()
                }
            }

        override suspend fun getBestPricesByTicker(ticker: String): Result<List<TickerBestPrice>> =
            withContext(ioDispatcher) {
                appRunCatching {
                    val response = cryptoCompareApi.getBestPricesByTicker(ticker)

                    checkApiResponse(response.errorCode, response.errorMsgs)

                    response.symbols.orEmpty().toTickerBestPrice()
                }
            }

        /**
         * Цены символов портфеля прямо из каталога: их и так держит свежими
         * сокет, а отдельного эндпоинта «цены по списку символов» у бэкенда нет.
         *
         * Нулевые и нечисловые цены отбрасываются здесь, а не в портфеле: ноль
         * у биржи означает «стороны стакана нет», и как стоимость позиции он
         * читался бы обнулением вложенного.
         */
        override fun observeSellQuotes(symbolIds: Set<Long>): Flow<Map<Long, SymbolSellQuote>> =
            symbolDao
                .observeSellQuotes(symbolIds.toList())
                .map { rows -> rows.toSellQuotes() }
                .distinctUntilChanged()

        override suspend fun applyBestPriceUpdates(updates: List<TickerBestPrice>): Result<Unit> =
            withContext(ioDispatcher) {
                appRunCatching {
                    symbolDao.updateBestPrices(
                        updates.map { update ->
                            SymbolBestPriceUpdate(
                                id = update.symbolId,
                                bestAskPrice = update.bestAskPrice,
                                bestAskProviderId = update.bestAskProviderId,
                                bestBidPrice = update.bestBidPrice,
                                bestBidProviderId = update.bestBidProviderId,
                                spreadPercent = update.spreadPercent,
                            )
                        },
                    )
                }
            }

        override suspend fun refreshCatalog(): Result<Unit> =
            withContext(ioDispatcher) {
                appRunCatching {
                    refreshSymbols()
                }
            }

        override suspend fun getCatalogLastUpdate(): Long = withContext(ioDispatcher) { symbolDao.getLastUpdate() }

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
                        sortBy = CryptoCompareRepositoryConstants.CATALOG_SORT_BY,
                        sortDir = CryptoCompareRepositoryConstants.CATALOG_SORT_DIR,
                    )
                checkApiResponse(response.errorCode, response.errorMsgs)

                // конец — пустая страница бэкенда, а не пустой остаток после отсева
                // строк без цены: страница, где отсеялось всё, обрывала бы выкачку,
                // и syncSymbols удалил бы весь каталог дальше неё
                val page = response.symbols.orEmpty()
                if (page.isEmpty()) break

                refreshedSymbols += page.normalizeSymbols().toEntityFromDto(syncedAtMillis)
                skip += CryptoCompareRepositoryConstants.SYMBOLS_IN_ROW
            }

            symbolDao.syncSymbols(refreshedSymbols)
        }

        private fun isCacheStale(lastUpdatedMillis: Long): Boolean =
            System.currentTimeMillis() - lastUpdatedMillis >=
                CryptoCompareRepositoryConstants.CATALOG_CACHE_TTL_MILLIS
    }
