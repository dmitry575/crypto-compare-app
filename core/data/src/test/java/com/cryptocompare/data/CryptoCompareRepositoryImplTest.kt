package com.cryptocompare.data

import com.cryptocompare.data.local.CryptoCompareDatabase
import com.cryptocompare.data.local.dao.ProviderDao
import com.cryptocompare.data.local.dao.SymbolDao
import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.data.repository.CryptoCompareRepositoryImpl
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.network.api.CryptoCompareApi
import com.cryptocompare.network.api.CryptoCompareHistoryApi
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.GetProvidersResponse
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.GetSymbolsResponse
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.ProviderDto
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto
import com.cryptocompare.network.dto.apiDTO.historyDTO.GetHistoryResponse
import com.cryptocompare.network.dto.apiDTO.historyDTO.HistoryCandleDto
import com.cryptocompare.network.dto.apiDTO.historyDTO.HistoryDataDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoCompareRepositoryImplTest {
    private val dispatcher = StandardTestDispatcher()

    private fun providerDto(
        id: Int,
        name: String? = "Provider$id",
        status: ProviderStatus = ProviderStatus.Enabled,
    ) = ProviderDto(
        id = id,
        name = name,
        webSite = "https://p$id.example",
        baseUrl = "https://api.p$id.example",
        status = status,
    )

    private fun symbolDto(
        id: Long,
        ticker: String,
        providerId: Int = 1,
        priceSell: Double = 101.0,
        priceBuy: Double = 99.0,
    ) = SymbolDto(
        id = id,
        ticker = ticker,
        symbol = ticker.uppercase(),
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
        updatedAt = "",
    )

    private fun createRepo(
        api: CryptoCompareApi = mockk(),
        symbolDao: SymbolDao = mockk(),
        providerDao: ProviderDao = mockk(),
        database: CryptoCompareDatabase = mockk(relaxed = true),
        historyApi: CryptoCompareHistoryApi = mockk(relaxed = true),
    ): CryptoCompareRepositoryImpl =
        CryptoCompareRepositoryImpl(
            cryptoCompareApi = api,
            historyApi = historyApi,
            database = database,
            symbolDao = symbolDao,
            providerDao = providerDao,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `getProviders returns success mapped providers when errorCode=0`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returns
                listOf(
                    ProviderEntity(1, "Binance", "https://p1.example", ProviderStatus.Enabled.name, 100L),
                    ProviderEntity(2, null, "https://p2.example", ProviderStatus.Enabled.name, 100L),
                )
            coEvery { providerDao.getLastUpdate() } returns 0L
            coEvery { providerDao.syncProviders(any()) } returns Unit

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providers = listOf(providerDto(1, name = "Binance"), providerDto(2, name = null)),
                )

            val result = repo.getProviders()

            assertTrue(result.isSuccess)
            val providers = result.getOrThrow()
            assertEquals(2, providers.size)
            assertEquals(1, providers[0].id)
            assertEquals("Binance", providers[0].name)
            assertEquals(ProviderStatus.Enabled, providers[0].status)
            assertEquals(2, providers[1].id)
            assertEquals(null, providers[1].name)
            coVerify(exactly = 1) { api.getProviders() }
        }

    @Test
    fun `getProviders returns cached providers when cache is fresh`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)
            val now = System.currentTimeMillis()

            coEvery { providerDao.getAll() } returns
                listOf(ProviderEntity(1, "Binance", "https://p1.example", ProviderStatus.Enabled.name, now))
            coEvery { providerDao.getLastUpdate() } returns now

            val result = repo.getProviders()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
            coVerify(exactly = 0) { api.getProviders() }
        }

    @Test
    fun `getProviders returns success empty list when providers is null`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returnsMany listOf(emptyList(), emptyList())
            coEvery { providerDao.getLastUpdate() } returns 0L
            coEvery { providerDao.syncProviders(any()) } returns Unit

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providers = null,
                )

            val result = repo.getProviders()

            assertTrue(result.isSuccess)
            assertEquals(emptyList<Provider>(), result.getOrThrow())
        }

    @Test
    fun `getProviders returns failure with joined error messages when errorCode != 0`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returnsMany listOf(emptyList(), emptyList())
            coEvery { providerDao.getLastUpdate() } returns 0L

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 10,
                    errorMsgs = listOf("E1", "E2"),
                    providers = null,
                )

            val result = repo.getProviders()

            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull()
            assertNotNull(ex)
            assertEquals("E1\nE2", ex!!.message)
        }

    @Test
    fun `getProviders returns failure with Unknown error when errorCode != 0 and errorMsgs null`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returnsMany listOf(emptyList(), emptyList())
            coEvery { providerDao.getLastUpdate() } returns 0L

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 1,
                    errorMsgs = null,
                    providers = null,
                )

            val result = repo.getProviders()

            assertTrue(result.isFailure)
            assertEquals("Unknown error", result.exceptionOrNull()!!.message)
        }

    @Test
    fun `getProviders returns cached providers when api throws exception`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returnsMany
                listOf(
                    emptyList(),
                    listOf(ProviderEntity(1, "Binance", "https://p1.example", ProviderStatus.Enabled.name, 100L)),
                    listOf(ProviderEntity(1, "Binance", "https://p1.example", ProviderStatus.Enabled.name, 100L)),
                )
            coEvery { providerDao.getLastUpdate() } returns 0L

            coEvery { api.getProviders() } throws IllegalStateException("boom")

            val result = repo.getProviders()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
        }

    @Test(expected = CancellationException::class)
    fun `getProviders rethrows CancellationException`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returns emptyList()
            coEvery { providerDao.getLastUpdate() } returns 0L
            coEvery { api.getProviders() } throws CancellationException("cancel")

            repo.getProviders()
        }

    @Test
    fun `refreshCatalog fetches symbol pages and writes merged result`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returnsMany
                listOf(
                    emptyList(),
                    listOf(ProviderEntity(1, "Provider1", "https://p1.example", ProviderStatus.Enabled.name, 100L)),
                    listOf(ProviderEntity(2, "Provider2", "https://p2.example", ProviderStatus.Enabled.name, 100L)),
                )
            coEvery { providerDao.getLastUpdate() } returns 0L
            coEvery { providerDao.syncProviders(any()) } returns Unit

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providers = listOf(providerDto(1), providerDto(2)),
                )

            coEvery { api.getSymbols(skip = 0, rows = 25) } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols = listOf(symbolDto(11L, "btcusdt")),
                )

            coEvery { api.getSymbols(skip = 25, rows = 25) } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols = listOf(symbolDto(21L, "ethusdt")),
                )

            coEvery { api.getSymbols(skip = 50, rows = 25) } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols = emptyList(),
                )

            val result = repo.refreshCatalog()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { symbolDao.syncSymbols(withArg { assertEquals(2, it.size) }) }
        }

    @Test
    fun `refreshCatalog normalizes providerId 0 to 1`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { providerDao.getAll() } returns emptyList()
            coEvery { providerDao.getLastUpdate() } returns 0L
            coEvery { providerDao.syncProviders(any()) } returns Unit

            coEvery { api.getProviders() } returns
                GetProvidersResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providers = listOf(providerDto(1)),
                )

            coEvery { api.getSymbols(skip = 0, rows = 25) } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols = listOf(symbolDto(11L, "btcusdt", providerId = 0)),
                )

            coEvery { api.getSymbols(skip = 25, rows = 25) } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols = emptyList(),
                )

            val result = repo.refreshCatalog()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                symbolDao.syncSymbols(withArg { assertEquals(1, it.single().providerId) })
            }
        }

    @Test
    fun `getSymbolsByTicker returns symbols from api`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>()
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { api.getSymbolsByTicker("btcusdt") } returns
                GetSymbolsResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    symbols =
                        listOf(
                            symbolDto(1L, "btcusdt", providerId = 3),
                            symbolDto(1L, "btc-usdt", providerId = 4),
                        ),
                )

            val result = repo.getSymbolsByTicker("btcusdt")

            assertTrue(result.isSuccess)
            assertEquals(listOf(3, 4), result.getOrThrow().map { it.providerId })
            coVerify(exactly = 0) { symbolDao.getByTicker(any()) }
        }

    @Test
    fun `getSymbolsByTicker falls back to cache when api fails`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>()
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { api.getSymbolsByTicker("btcusdt") } throws java.net.SocketTimeoutException("timeout")
            coEvery { symbolDao.getByTicker("btcusdt") } returns
                listOf(
                    com.cryptocompare.data.local.entity.SymbolEntity(
                        id = 1L,
                        ticker = "btcusdt",
                        symbol = "btc/usdt",
                        providerId = 1,
                        priceSell = 101.0,
                        priceBuy = 99.0,
                        updatedAt = "",
                        syncedAtMillis = 100L,
                    ),
                )

            val result = repo.getSymbolsByTicker("btcusdt")

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
        }

    @Test
    fun `getCandles splits ticker and issues a single aggregate request`() =
        runTest(dispatcher) {
            val historyApi = mockk<CryptoCompareHistoryApi>()
            val repo = createRepo(historyApi = historyApi)

            coEvery { historyApi.getDailyHistory("BTC", "USDT", any(), any()) } returns
                GetHistoryResponse(
                    response = "Success",
                    message = null,
                    error = null,
                    data = HistoryDataDto(listOf(HistoryCandleDto(1_751_328_000L, 1.0, 1.5, 0.5, 1.2))),
                )

            val result = repo.getCandles("btcusdt", ChartTimeframe.D1)

            assertTrue(result.isSuccess)
            val candle = result.getOrThrow().single()
            assertEquals(1.0, candle.open, 0.0)
            assertEquals(1.2, candle.close, 0.0)
            // время приходит в секундах и разворачивается в миллисекунды
            assertEquals(1_751_328_000_000L, candle.timeMillis)
            coVerify(exactly = 1) { historyApi.getDailyHistory(any(), any(), any(), any()) }
        }

    @Test
    fun `getCandles drops empty zero candles`() =
        runTest(dispatcher) {
            val historyApi = mockk<CryptoCompareHistoryApi>()
            val repo = createRepo(historyApi = historyApi)

            coEvery { historyApi.getDailyHistory("BTC", "USDT", any(), any()) } returns
                GetHistoryResponse(
                    response = "Success",
                    message = null,
                    error = null,
                    data =
                        HistoryDataDto(
                            listOf(
                                HistoryCandleDto(1_751_328_000L, 0.0, 0.0, 0.0, 0.0),
                                HistoryCandleDto(1_751_414_400L, 1.0, 2.0, 0.5, 1.5),
                            ),
                        ),
                )

            val result = repo.getCandles("btcusdt", ChartTimeframe.D1)

            assertEquals(1, result.getOrThrow().size)
        }

    @Test
    fun `getCandles returns failure when api reports an error`() =
        runTest(dispatcher) {
            val historyApi = mockk<CryptoCompareHistoryApi>()
            val repo = createRepo(historyApi = historyApi)

            coEvery { historyApi.getDailyHistory(any(), any(), any(), any()) } returns
                GetHistoryResponse("Error", "API key required", null, null)

            val result = repo.getCandles("btcusdt", ChartTimeframe.D1)

            assertTrue(result.isFailure)
            assertEquals("API key required", result.exceptionOrNull()!!.message)
        }

    @Test
    fun `getCandles fails on unsupported ticker format`() =
        runTest(dispatcher) {
            val result = createRepo().getCandles("zzz", ChartTimeframe.D1)

            assertTrue(result.isFailure)
        }

    @Test
    fun `applyPriceUpdates writes batch to dao`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>(relaxed = true)
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            val updates =
                listOf(
                    TickerPrice(
                        ticker = "btcusdt",
                        symbolId = 11,
                        providerId = 1,
                        priceSell = 105.0,
                        priceBuy = 100.0,
                    ),
                    TickerPrice(
                        ticker = "ethusdt",
                        symbolId = 21,
                        providerId = 1,
                        priceSell = 12.0,
                        priceBuy = 11.0,
                    ),
                )

            val result = repo.applyPriceUpdates(updates)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                symbolDao.updatePrices(
                    listOf(
                        Triple(11L, 100.0, 105.0),
                        Triple(21L, 11.0, 12.0),
                    ),
                )
            }
        }

    @Test
    fun `applyPriceUpdates returns failure when dao throws`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val symbolDao = mockk<SymbolDao>()
            val providerDao = mockk<ProviderDao>()
            val repo = createRepo(api, symbolDao, providerDao)

            coEvery { symbolDao.updatePrices(any()) } throws IllegalStateException("db is closed")

            val result =
                repo.applyPriceUpdates(
                    listOf(
                        TickerPrice(
                            ticker = "btcusdt",
                            symbolId = 11,
                            providerId = 1,
                            priceSell = 105.0,
                            priceBuy = 100.0,
                        ),
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals("db is closed", result.exceptionOrNull()!!.message)
        }
}
