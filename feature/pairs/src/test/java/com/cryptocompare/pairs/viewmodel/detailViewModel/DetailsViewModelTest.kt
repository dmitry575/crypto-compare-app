package com.cryptocompare.pairs.viewmodel.detailViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cryptocompare.domain.usecase.pairs.GetTickerDetailUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerHistoryUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.SubscribeSingleTickerUseCase
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerDetail
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val events = MutableSharedFlow<TickerStreamEvent>(extraBufferCapacity = 16)

    // часовые бары открываются на кратных H1_DURATION_MS отметках, поэтому живой
    // тик попадает в последний бар независимо от того, в какой момент идёт тест
    private val hourBucket =
        System.currentTimeMillis() -
            System.currentTimeMillis() % PairsConstants.Chart.H1_DURATION_MS

    private fun providerDetail(
        id: Int,
        priceSell: Double?,
        priceBuy: Double?,
    ) = ProviderDetail(
        provider = Provider(id = id, name = "provider$id", webSite = null, status = ProviderStatus.Enabled),
        priceSell = priceSell,
        priceBuy = priceBuy,
    )

    private fun candle(
        timeMillis: Long,
        close: Double,
    ) = Candle(
        timeMillis = timeMillis,
        open = close,
        high = close,
        low = close,
        close = close,
    )

    private fun connectUseCaseMock(): StreamConnectUseCase = mockk(relaxed = true)

    private fun subscribeSingleUseCaseMock(): SubscribeSingleTickerUseCase = mockk(relaxed = true)

    private fun restoreUseCaseMock(): RestoreTickerSubscriptionsUseCase = mockk(relaxed = true)

    private fun detailsUseCaseMock(exchanges: List<ProviderDetail>): GetTickerDetailUseCase =
        mockk {
            coEvery { this@mockk.invoke(any()) } returns
                Result.success(TickerDetail(ticker = "btcusdt", exchanges = exchanges))
        }

    private fun historyUseCaseMock(candles: List<Candle>): GetTickerHistoryUseCase =
        mockk { coEvery { this@mockk.invoke(any(), any(), any(), any(), any()) } returns Result.success(candles) }

    private fun observeEventsUseCaseMock(): ObserveTickerEventUseCase =
        mockk { every { this@mockk.invoke() } returns events }

    private fun defaultExchanges(): List<ProviderDetail> =
        listOf(
            providerDetail(id = 1, priceSell = 100.0, priceBuy = 99.0),
            providerDetail(id = 2, priceSell = 101.0, priceBuy = 100.5),
        )

    private fun defaultHistory(): List<Candle> =
        listOf(
            candle(hourBucket - PairsConstants.Chart.H1_DURATION_MS, close = 100.0),
            candle(hourBucket, close = 104.0),
        )

    private fun makeVm(
        connect: StreamConnectUseCase = connectUseCaseMock(),
        subscribeSingle: SubscribeSingleTickerUseCase = subscribeSingleUseCaseMock(),
        restore: RestoreTickerSubscriptionsUseCase = restoreUseCaseMock(),
        details: GetTickerDetailUseCase = detailsUseCaseMock(defaultExchanges()),
        history: GetTickerHistoryUseCase = historyUseCaseMock(defaultHistory()),
        observeEvents: ObserveTickerEventUseCase = observeEventsUseCaseMock(),
    ): DetailsViewModel =
        DetailsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(PairsConstants.Navigation.TICKER_ARG to "BTCUSDT")),
            getPairDetailsUseCase = details,
            getTickerHistoryUseCase = history,
            streamConnectUseCase = connect,
            subscribeSingleTickerUseCase = subscribeSingle,
            restoreTickerSubscriptionsUseCase = restore,
            observeTickerEventUseCase = observeEvents,
        )

    private fun tick(
        priceSell: Double,
        priceBuy: Double,
        ticker: String = "btcusdt",
        providerId: Int = 1,
    ) = TickerStreamEvent.TickerPriceChange(
        id = "evt",
        data =
            TickerPrice(
                ticker = ticker,
                symbolId = 1,
                providerId = providerId,
                priceSell = priceSell,
                priceBuy = priceBuy,
            ),
    )

    @Test
    fun `init connects and takes over subscriptions for the pair ticker`() =
        runTest {
            val connect = connectUseCaseMock()
            val subscribeSingle = subscribeSingleUseCaseMock()

            makeVm(connect = connect, subscribeSingle = subscribeSingle)
            runCurrent()

            verify(exactly = 1) { connect.invoke() }
            verify(exactly = 1) { subscribeSingle.invoke("btcusdt") }
        }

    @Test
    fun `price tick moves the last bar after the sample interval`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.tryEmit(tick(priceSell = 110.0, priceBuy = 108.0))
            runCurrent()

            // до конца интервала сэмплирования бар не двигается
            assertEquals(
                104.0,
                vm.uiState.value.candles
                    .last()
                    .close,
                0.0,
            )

            advanceTimeBy(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS + 1)
            runCurrent()

            // середина спреда (110 + 108) / 2 поднимает и закрытие, и максимум бара
            assertEquals(
                109.0,
                vm.uiState.value.candles
                    .last()
                    .close,
                0.0,
            )
            assertEquals(
                109.0,
                vm.uiState.value.candles
                    .last()
                    .high,
                0.0,
            )
        }

    @Test
    fun `ticks of other tickers are ignored`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.tryEmit(tick(priceSell = 200.0, priceBuy = 190.0, ticker = "ethusdt"))
            advanceTimeBy(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS + 1)
            runCurrent()

            assertEquals(
                104.0,
                vm.uiState.value.candles
                    .last()
                    .close,
                0.0,
            )
        }

    @Test
    fun `tick updates prices of the matching exchange card only`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.tryEmit(tick(providerId = 2, priceSell = 105.0, priceBuy = 104.0))
            advanceTimeBy(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS + 1)
            runCurrent()

            assertEquals(
                100.0,
                vm.uiState.value.exchanges
                    .first()
                    .priceSell!!,
                0.0,
            )
            assertEquals(
                105.0,
                vm.uiState.value.exchanges
                    .last()
                    .priceSell!!,
                0.0,
            )
            assertEquals(
                104.0,
                vm.uiState.value.exchanges
                    .last()
                    .priceBuy!!,
                0.0,
            )
        }

    @Test
    fun `selecting another exchange reloads the chart for that provider`() =
        runTest {
            val history = mockk<GetTickerHistoryUseCase>()
            coEvery { history.invoke(1, any(), any(), any(), any()) } returns
                Result.success(listOf(candle(hourBucket, close = 104.0)))
            coEvery { history.invoke(2, any(), any(), any(), any()) } returns
                Result.success(listOf(candle(hourBucket, close = 222.0)))

            val vm = makeVm(history = history)
            runCurrent()
            // по умолчанию график первой биржи (id 1)
            assertEquals(
                104.0,
                vm.uiState.value.candles
                    .last()
                    .close,
                0.0,
            )

            vm.onExchangeSelected(1) // вторая биржа — id 2
            runCurrent()

            assertEquals(
                222.0,
                vm.uiState.value.candles
                    .last()
                    .close,
                0.0,
            )
            coVerify(exactly = 1) { history.invoke(2, "btcusdt", any(), any(), 0) }
        }

    @Test
    fun `the first chart page is requested once per exchange and timeframe`() =
        runTest {
            val history = historyUseCaseMock(defaultHistory())

            val vm = makeVm(history = history)
            runCurrent()

            coVerify(exactly = 1) {
                history.invoke(1, "btcusdt", any(), PairsConstants.Chart.PAGE_LIMIT, 0)
            }

            vm.onExchangeSelected(1)
            runCurrent()
            vm.onExchangeSelected(0)
            runCurrent()

            // возврат на прежнюю биржу берёт свечи из памяти, а не из сети
            coVerify(exactly = 1) { history.invoke(1, "btcusdt", any(), any(), 0) }
            assertEquals(defaultHistory().size, vm.uiState.value.candles.size)
        }

    @Test
    fun `an older page is prepended and the next offset skips what is loaded`() =
        runTest {
            val initial = listOf(candle(3000L, close = 30.0), candle(4000L, close = 40.0))
            val older = listOf(candle(1000L, close = 10.0), candle(2000L, close = 20.0))
            val history = mockk<GetTickerHistoryUseCase>()
            coEvery { history.invoke(1, "btcusdt", any(), any(), 0) } returns Result.success(initial)
            coEvery { history.invoke(1, "btcusdt", any(), any(), 2) } returns Result.success(older)

            val vm = makeVm(history = history)
            runCurrent()
            assertEquals(2, vm.uiState.value.candles.size)

            vm.loadOlderCandles()
            runCurrent()

            val candles = vm.uiState.value.candles
            assertEquals(4, candles.size)
            assertEquals(1000L, candles.first().timeMillis)
            assertEquals(4000L, candles.last().timeMillis)
            assertFalse(vm.uiState.value.chartLoadingOlder)
            assertTrue(vm.uiState.value.chartCanLoadOlder)
        }

    @Test
    fun `an empty older page stops the paging`() =
        runTest {
            val history = mockk<GetTickerHistoryUseCase>()
            coEvery { history.invoke(any(), any(), any(), any(), 0) } returns
                Result.success(listOf(candle(4000L, close = 40.0)))
            coEvery { history.invoke(any(), any(), any(), any(), 1) } returns Result.success(emptyList())

            val vm = makeVm(history = history)
            runCurrent()
            assertTrue(vm.uiState.value.chartCanLoadOlder)

            vm.loadOlderCandles()
            runCurrent()

            assertFalse(vm.uiState.value.chartCanLoadOlder)
            // дальше просить бессмысленно — второй раз в сеть не идём
            vm.loadOlderCandles()
            runCurrent()
            coVerify(exactly = 1) { history.invoke(any(), any(), any(), any(), 1) }
        }

    @Test
    fun `a live tick that opens a bar is counted separately from server candles`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            // тик уходит в существующий бар: живых свечей не прибавляется
            events.emit(tick(priceSell = 106.0, priceBuy = 104.0))
            advanceTimeBy(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS + 1)
            runCurrent()

            assertEquals(0, vm.uiState.value.liveCount)
            assertEquals(defaultHistory().size, vm.uiState.value.candles.size)
        }

    @Test
    fun `a failed history request leaves the screen usable`() =
        runTest {
            val history =
                mockk<GetTickerHistoryUseCase> {
                    coEvery { this@mockk.invoke(any(), any(), any(), any(), any()) } returns
                        Result.failure(IllegalStateException("boom"))
                }

            val vm = makeVm(history = history)
            runCurrent()

            val state = vm.uiState.value
            assertFalse(state.chartLoading)
            assertTrue(state.candles.isEmpty())
            // биржи и цены остаются на экране, ошибка графика их не сносит
            assertEquals(2, state.exchanges.size)
            assertEquals(null, state.error)
        }

    @Test
    fun `clearing the viewmodel ends the subscription takeover`() =
        runTest {
            val restore = restoreUseCaseMock()

            val vm = makeVm(restore = restore)
            runCurrent()

            clearViewModel(vm)

            // база каталога живёт в репозитории — VM лишь отпускает захват
            verify(exactly = 1) { restore.invoke() }
        }

    /** onCleared() защищён, поэтому дёргаем его через настоящий ViewModelStore. */
    private fun clearViewModel(viewModel: ViewModel) {
        val store = object : ViewModelStore() {}
        val provider =
            ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
                },
            )
        provider[DetailsViewModel::class.java]
        store.clear()
    }
}
