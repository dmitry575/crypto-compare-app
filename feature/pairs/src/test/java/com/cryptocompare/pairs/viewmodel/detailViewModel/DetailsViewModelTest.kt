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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun subscribeSingleUseCaseMock(
        previous: Set<String> = setOf("ethusdt", "adausdt"),
    ): SubscribeSingleTickerUseCase = mockk { every { this@mockk.invoke(any()) } returns previous }

    private fun restoreUseCaseMock(): RestoreTickerSubscriptionsUseCase = mockk(relaxed = true)

    private fun detailsUseCaseMock(exchanges: List<ProviderDetail>): GetTickerDetailUseCase =
        mockk {
            coEvery { this@mockk.invoke(any()) } returns
                Result.success(TickerDetail(ticker = "btcusdt", exchanges = exchanges))
        }

    private fun historyUseCaseMock(candles: List<Candle>): GetTickerHistoryUseCase =
        mockk { coEvery { this@mockk.invoke(any(), any()) } returns Result.success(candles) }

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
            val subscribeSingle = subscribeSingleUseCaseMock(previous = emptySet())

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
    fun `clearing the viewmodel restores previous catalog subscriptions`() =
        runTest {
            val restore = restoreUseCaseMock()
            val subscribeSingle = subscribeSingleUseCaseMock(previous = setOf("ethusdt", "adausdt"))

            val vm = makeVm(subscribeSingle = subscribeSingle, restore = restore)
            runCurrent()

            clearViewModel(vm)

            verify(exactly = 1) { restore.invoke(setOf("ethusdt", "adausdt")) }
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
