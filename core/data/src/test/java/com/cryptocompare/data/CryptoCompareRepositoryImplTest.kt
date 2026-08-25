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
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.GetProvidersResponse
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.GetSymbolsResponse
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.ProviderDto
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto
import com.cryptocompare.network.dto.apiDTO.klinesDTO.GetKlinesResponse
import com.cryptocompare.network.dto.apiDTO.klinesDTO.KlineEntryDto
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
    ): CryptoCompareRepositoryImpl =
        CryptoCompareRepositoryImpl(
            cryptoCompareApi = api,
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

    private fun kline(
        openTime: String,
        open: Double = 1.0,
        high: Double = 1.5,
        low: Double = 0.5,
        close: Double = 1.2,
    ) = KlineEntryDto(
        openTime = openTime,
        openPrice = open,
        highPrice = high,
        lowPrice = low,
        closePrice = close,
        volume = 10.0,
    )

    @Test
    fun `getCandles requests a klines page for the provider and maps the response`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val repo = createRepo(api = api)

            coEvery {
                api.getKlines(providerId = 7, symbol = "btcusdt", interval = "1d", limit = 300, offset = 0)
            } returns
                GetKlinesResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providerId = 7,
                    providerName = "mexc",
                    ticker = "btcusdt",
                    interval = "1d",
                    klines = listOf(kline("2026-07-01T00:00:00Z", open = 1.0, close = 1.2)),
                )

            val result =
                repo.getCandles(
                    providerId = 7,
                    symbol = "btcusdt",
                    timeframe = ChartTimeframe.D1,
                    limit = 300,
                    offset = 0,
                )

            assertTrue(result.isSuccess)
            val candle = result.getOrThrow().single()
            assertEquals(1.0, candle.open, 0.0)
            assertEquals(1.2, candle.close, 0.0)
            assertEquals(
                java.time.Instant
                    .parse("2026-07-01T00:00:00Z")
                    .toEpochMilli(),
                candle.timeMillis,
            )
            coVerify(exactly = 1) {
                api.getKlines(providerId = 7, symbol = "btcusdt", interval = "1d", limit = 300, offset = 0)
            }
        }

    @Test
    fun `getCandles drops empty zero candles and sorts by time`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val repo = createRepo(api = api)

            coEvery { api.getKlines(any(), any(), any(), any(), any()) } returns
                GetKlinesResponse(
                    errorCode = 0,
                    errorMsgs = null,
                    providerId = 1,
                    providerName = null,
                    ticker = "btcusdt",
                    interval = "1d",
                    klines =
                        listOf(
                            kline("2026-07-02T00:00:00Z", open = 1.0, high = 2.0, low = 0.5, close = 1.5),
                            kline("2026-07-01T00:00:00Z", open = 0.0, high = 0.0, low = 0.0, close = 0.0),
                        ),
                )

            val candles = repo.getCandles(1, "btcusdt", ChartTimeframe.D1, 300, 0).getOrThrow()

            assertEquals(1, candles.size)
            assertEquals(1.5, candles.single().close, 0.0)
        }

    @Test
    fun `getCandles returns an empty page unchanged as the end-of-history signal`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val repo = createRepo(api = api)

            coEvery { api.getKlines(any(), any(), any(), any(), any()) } returns
                GetKlinesResponse(0, null, 1, null, "btcusdt", "1d", emptyList())

            val result = repo.getCandles(1, "btcusdt", ChartTimeframe.D1, 300, 900)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().isEmpty())
        }

    @Test
    fun `getCandles returns failure when api reports an error`() =
        runTest(dispatcher) {
            val api = mockk<CryptoCompareApi>()
            val repo = createRepo(api = api)

            coEvery { api.getKlines(any(), any(), any(), any(), any()) } returns
                GetKlinesResponse(-2, listOf("Invalid request"), null, null, null, null, null)

            val result = repo.getCandles(1, "btcusdt", ChartTimeframe.D1, 300, 0)

            assertTrue(result.isFailure)
            assertEquals("Invalid request", result.exceptionOrNull()!!.message)
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
