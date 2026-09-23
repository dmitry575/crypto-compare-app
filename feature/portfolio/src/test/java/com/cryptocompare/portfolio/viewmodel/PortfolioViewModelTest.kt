package com.cryptocompare.portfolio.viewmodel

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.domain.usecase.pairs.ApplyBestPriceChangesUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveStreamReconnectsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RefreshBestPricesUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.SyncVisibleTickersUseCase
import com.cryptocompare.domain.usecase.pairs.TakeOverTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.portfolio.ApplyPinnedPriceTicksUseCase
import com.cryptocompare.domain.usecase.portfolio.CalculatePortfolioUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioPricesUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import com.cryptocompare.domain.usecase.portfolio.RefreshPinnedQuotesUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.symbol.SymbolSellQuote
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.portfolio.util.PortfolioConstants
import com.cryptocompare.portfolio.viewmodel.portfolioviewmodel.PortfolioViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val portfolioRepository: PortfolioRepository = mockk()
    private val cryptoCompareRepository: CryptoCompareRepository = mockk(relaxed = true)
    private val tickerStreamRepository: TickerStreamRepository = mockk(relaxed = true)

    private val streamConnect: StreamConnectUseCase = mockk(relaxed = true)
    private val takeOverSubscriptions: TakeOverTickerSubscriptionsUseCase = mockk(relaxed = true)
    private val restoreSubscriptions: RestoreTickerSubscriptionsUseCase = mockk(relaxed = true)
    private val refreshBestPrices: RefreshBestPricesUseCase = mockk(relaxed = true)
    private val applyBestPriceChanges: ApplyBestPriceChangesUseCase = mockk(relaxed = true)
    private val refreshPinnedQuotes: RefreshPinnedQuotesUseCase = mockk(relaxed = true)
    private val applyPinnedPriceTicks: ApplyPinnedPriceTicksUseCase = mockk(relaxed = true)

    private val events = MutableSharedFlow<TickerStreamEvent>()
    private val reconnects = MutableSharedFlow<Unit>()

    @Before
    fun setUp() {
        coEvery { refreshBestPrices.invoke(any()) } returns Result.success(1)
        coEvery { refreshPinnedQuotes.invoke(any()) } returns Result.success(1)
    }

    @Test
    fun `until the database answers the screen is loading, not empty`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellQuotes(any()) } returns MutableStateFlow(emptyMap())

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loading)

            positions.emit(emptyList())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.loading)
            assertTrue(state.holdings.isEmpty())
            assertNull(state.summary)
        }

    @Test
    fun `positions follow the database and get priced`() =
        runTest {
            val positions = MutableSharedFlow<List<PortfolioPosition>>()
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellQuotes(setOf(SYMBOL_ID)) } returns
                MutableStateFlow(mapOf(SYMBOL_ID to quote(80_000.0)))

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            positions.emit(listOf(POSITION))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val holding = state.holdings.single()
            assertEquals(POSITION, holding.position)
            assertEquals(33_600.0, holding.currentValue!!, DELTA)
            assertEquals(3_360.0, state.summary!!.profit, DELTA)
        }

    @Test
    fun `a price tick repriced the portfolio without touching the positions`() =
        runTest {
            // цены приходят из каталога, который двигает сокет: позиции при этом
            // в базе не меняются, и без пересчёта итог застыл бы на входном
            val prices = MutableStateFlow(mapOf(SYMBOL_ID to quote(80_000.0)))
            every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(POSITION))
            every { cryptoCompareRepository.observeSellQuotes(setOf(SYMBOL_ID)) } returns prices

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            prices.value = mapOf(SYMBOL_ID to quote(60_000.0))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(25_200.0, state.holdings.single().currentValue!!, DELTA)
            assertEquals(-5_040.0, state.summary!!.profit, DELTA)
        }

    @Test
    fun `the screen takes the subscription slots and gives them back on leave`() =
        runTest {
            givenPortfolio()
            val viewModel = createViewModel()

            viewModel.onScreenShown()
            advanceUntilIdle()

            verify(exactly = 1) { streamConnect.invoke() }
            verify(exactly = 1) { takeOverSubscriptions.invoke(emptySet()) }
            // каталога под портфелем не видно, слоты достаются его парам
            verify(exactly = 1) { tickerStreamRepository.subscribe("btcusdt") }

            viewModel.onScreenHidden()
            advanceUntilIdle()

            verify(exactly = 1) { restoreSubscriptions.invoke() }
        }

    @Test
    fun `opening the screen pulls prices for pairs the catalog never showed`() =
        runTest {
            givenPortfolio()
            val viewModel = createViewModel()

            viewModel.onScreenShown()
            advanceUntilIdle()

            coVerify(exactly = 1) { refreshBestPrices.invoke(setOf("btcusdt")) }
        }

    @Test
    fun `a reconnect pulls the prices again`() =
        runTest {
            // за время разрыва тики потеряны, а сервер их не досылает
            givenPortfolio()
            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            reconnects.emit(Unit)
            advanceUntilIdle()

            coVerify(exactly = 2) { refreshBestPrices.invoke(setOf("btcusdt")) }
        }

    @Test
    fun `best price ticks reach the catalog in batches`() =
        runTest {
            givenPortfolio()
            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            events.emit(TickerStreamEvent.TickerBestPriceChange(id = "1", data = bestPrice(81_000.0)))
            events.emit(TickerStreamEvent.TickerBestPriceChange(id = "2", data = bestPrice(82_000.0)))
            advanceTimeBy(PortfolioConstants.Prices.FLUSH_INTERVAL_MS + 1)

            // за интервал накопился один символ — в базу уходит его последняя цена
            coVerify(exactly = 1) { applyBestPriceChanges.invoke(listOf(bestPrice(82_000.0))) }
        }

    @Test
    fun `ticks stop reaching the catalog once the screen is gone`() =
        runTest {
            givenPortfolio()
            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            viewModel.onScreenHidden()
            events.emit(TickerStreamEvent.TickerBestPriceChange(id = "1", data = bestPrice(81_000.0)))
            advanceTimeBy(PortfolioConstants.Prices.FLUSH_INTERVAL_MS + 1)

            coVerify(exactly = 0) { applyBestPriceChanges.invoke(any()) }
        }

    @Test
    fun `a hidden screen does not recount the portfolio`() =
        runTest {
            // ViewModel вкладки переживает уход с неё: без остановки портфель
            // пересчитывался бы на каждый сброс цен каталога, никому не показываясь
            val prices = MutableStateFlow(mapOf(SYMBOL_ID to quote(80_000.0)))
            every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(POSITION))
            every { cryptoCompareRepository.observeSellQuotes(setOf(SYMBOL_ID)) } returns prices

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()
            viewModel.onScreenHidden()

            prices.value = mapOf(SYMBOL_ID to quote(60_000.0))
            advanceUntilIdle()

            // на экране осталось то, что было при уходе, — и это не мигнёт загрузкой
            assertEquals(
                33_600.0,
                viewModel.uiState.value.holdings
                    .single()
                    .currentValue!!,
                DELTA,
            )
        }

    @Test
    fun `a catch-up that applied nothing is tried again`() =
        runTest {
            // офлайн: use case глотает ошибки по тикерам и отдаёт success(0).
            // Пометить такие цены догнанными значило бы больше за ними не пойти
            val positions = MutableStateFlow(listOf(POSITION))
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellQuotes(any()) } returns
                MutableStateFlow(mapOf(SYMBOL_ID to quote(80_000.0)))
            coEvery { refreshBestPrices.invoke(any()) } returns Result.success(0)

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            positions.value = listOf(POSITION.copy(amount = 1.0))
            advanceUntilIdle()

            coVerify(exactly = 2) { refreshBestPrices.invoke(setOf("btcusdt")) }
        }

    @Test
    fun `a position bought on an exchange is priced by that exchange, not the best bid`() =
        runTest {
            // лучший bid сейчас на bitget, но монета лежит на bybit, и продать её
            // можно только там
            every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(PINNED))
            every { cryptoCompareRepository.observeSellQuotes(emptySet()) } returns MutableStateFlow(emptyMap())
            every { cryptoCompareRepository.observeSellQuotes(setOf(SYMBOL_ID)) } returns
                MutableStateFlow(mapOf(SYMBOL_ID to quote(81_200.0)))
            every { portfolioRepository.observePinnedQuotes() } returns
                MutableStateFlow(mapOf(SYMBOL_ID to SymbolSellQuote(81_050.0, BYBIT, "bybit")))

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            val holding =
                viewModel.uiState.value.holdings
                    .single()
            assertEquals(81_050.0, holding.currentPrice!!, DELTA)
            assertEquals("bybit", holding.priceExchange)
        }

    @Test
    fun `opening the screen pulls the price of the position's own exchange`() =
        runTest {
            givenPinnedPortfolio()
            val viewModel = createViewModel()

            viewModel.onScreenShown()
            advanceUntilIdle()

            // лучшие пары ей не нужны: цена у неё только с bybit
            coVerify(exactly = 1) { refreshPinnedQuotes.invoke(listOf(PINNED)) }
            coVerify(exactly = 0) { refreshBestPrices.invoke(any()) }
        }

    @Test
    fun `a new exchange on a position is a new price to pull`() =
        runTest {
            val positions = MutableStateFlow(listOf(PINNED))
            every { portfolioRepository.observePositions() } returns positions
            every { cryptoCompareRepository.observeSellQuotes(any()) } returns MutableStateFlow(emptyMap())
            every { portfolioRepository.observePinnedQuotes() } returns MutableStateFlow(emptyMap())

            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            val moved = PINNED.copy(providerId = OKX)
            positions.value = listOf(moved)
            advanceUntilIdle()

            coVerify(exactly = 1) { refreshPinnedQuotes.invoke(listOf(moved)) }
        }

    @Test
    fun `only ticks of the position's exchange are kept as its price`() =
        runTest {
            givenPinnedPortfolio()
            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            events.emit(TickerStreamEvent.TickerPriceChange(id = "1", data = exchangeTick(OKX, 81_300.0)))
            events.emit(TickerStreamEvent.TickerPriceChange(id = "2", data = exchangeTick(BYBIT, 81_000.0)))
            events.emit(TickerStreamEvent.TickerPriceChange(id = "3", data = exchangeTick(BYBIT, 81_050.0)))
            advanceTimeBy(PortfolioConstants.Prices.FLUSH_INTERVAL_MS + 1)

            // чужая биржа отброшена, из своих в базу уходит последний тик за интервал
            coVerify(exactly = 1) { applyPinnedPriceTicks.invoke(listOf(exchangeTick(BYBIT, 81_050.0))) }
            coVerify(exactly = 0) { applyBestPriceChanges.invoke(any()) }
        }

    @Test
    fun `exchange ticks are ignored for a position without an exchange`() =
        runTest {
            givenPortfolio()
            val viewModel = createViewModel()
            viewModel.onScreenShown()
            advanceUntilIdle()

            events.emit(TickerStreamEvent.TickerPriceChange(id = "1", data = exchangeTick(BYBIT, 81_000.0)))
            advanceTimeBy(PortfolioConstants.Prices.FLUSH_INTERVAL_MS + 1)

            coVerify(exactly = 0) { applyPinnedPriceTicks.invoke(any()) }
        }

    private fun givenPinnedPortfolio() {
        every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(PINNED))
        every { cryptoCompareRepository.observeSellQuotes(any()) } returns MutableStateFlow(emptyMap())
        every { portfolioRepository.observePinnedQuotes() } returns
            MutableStateFlow(mapOf(SYMBOL_ID to SymbolSellQuote(81_050.0, BYBIT, "bybit")))
    }

    private fun givenPortfolio() {
        every { portfolioRepository.observePositions() } returns MutableStateFlow(listOf(POSITION))
        every { cryptoCompareRepository.observeSellQuotes(setOf(SYMBOL_ID)) } returns
            MutableStateFlow(mapOf(SYMBOL_ID to quote(80_000.0)))
    }

    private fun createViewModel() =
        PortfolioViewModel(
            observePortfolioUseCase = ObservePortfolioUseCase(portfolioRepository),
            observePortfolioPricesUseCase = ObservePortfolioPricesUseCase(cryptoCompareRepository, portfolioRepository),
            calculatePortfolioUseCase = CalculatePortfolioUseCase(),
            streamConnectUseCase = streamConnect,
            takeOverTickerSubscriptionsUseCase = takeOverSubscriptions,
            restoreTickerSubscriptionsUseCase = restoreSubscriptions,
            syncVisibleTickersUseCase = SyncVisibleTickersUseCase(tickerStreamRepository),
            observeTickerEventUseCase = observeTickerEventUseCase(),
            observeStreamReconnectsUseCase = observeStreamReconnectsUseCase(),
            applyBestPriceChangesUseCase = applyBestPriceChanges,
            refreshBestPricesUseCase = refreshBestPrices,
            refreshPinnedQuotesUseCase = refreshPinnedQuotes,
            applyPinnedPriceTicksUseCase = applyPinnedPriceTicks,
        )

    private fun observeTickerEventUseCase(): ObserveTickerEventUseCase {
        val useCase: ObserveTickerEventUseCase = mockk()
        every { useCase.invoke() } returns events
        return useCase
    }

    private fun observeStreamReconnectsUseCase(): ObserveStreamReconnectsUseCase {
        val useCase: ObserveStreamReconnectsUseCase = mockk()
        every { useCase.invoke() } returns reconnects
        return useCase
    }

    private fun quote(price: Double) = SymbolSellQuote(price = price, providerId = 2, exchangeName = "bitget")

    private fun exchangeTick(
        providerId: Int,
        bid: Double,
    ) = TickerPrice(
        ticker = "BTCUSDT",
        symbolId = SYMBOL_ID.toInt(),
        providerId = providerId,
        priceSell = bid + 1,
        priceBuy = bid,
    )

    private fun bestPrice(price: Double) =
        TickerBestPrice(
            ticker = "BTCUSDT",
            symbolId = SYMBOL_ID,
            bestAskProviderId = 1,
            bestAskPrice = price + 1,
            bestBidProviderId = 2,
            bestBidPrice = price,
            spreadPercent = -0.1,
        )

    private companion object {
        const val SYMBOL_ID = 1L
        const val BYBIT = 5
        const val OKX = 7
        const val DELTA = 1e-9

        val POSITION =
            PortfolioPosition(
                symbolId = SYMBOL_ID,
                ticker = "BTCUSDT",
                amount = 0.42,
                buyPrice = 72_000.0,
                updatedAtMillis = 1_700_000_000_000L,
            )

        val PINNED = POSITION.copy(providerId = BYBIT, exchangeName = "bybit")
    }
}
