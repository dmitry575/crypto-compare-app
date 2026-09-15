package com.cryptocompare.pairs.viewmodel.comparisonViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cryptocompare.domain.usecase.pairs.ApplyComparisonBestPricesUseCase
import com.cryptocompare.domain.usecase.pairs.ComparePairAcrossExchangesUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveStreamReconnectsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.SubscribeSingleTickerUseCase
import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.model.ticker.TickerBestPrice
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val events = MutableSharedFlow<TickerStreamEvent>(extraBufferCapacity = 16)
    private val reconnects = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val observeReconnects: ObserveStreamReconnectsUseCase =
        mockk { every { this@mockk.invoke() } returns reconnects }

    private val connect: StreamConnectUseCase = mockk(relaxed = true)
    private val subscribeSingle: SubscribeSingleTickerUseCase = mockk(relaxed = true)
    private val restore: RestoreTickerSubscriptionsUseCase = mockk(relaxed = true)
    private val observeEvents: ObserveTickerEventUseCase = mockk { every { this@mockk.invoke() } returns events }
    private val compare: ComparePairAcrossExchangesUseCase =
        mockk { coEvery { this@mockk.invoke(TICKER) } returns Result.success(defaultComparison()) }

    private fun makeVm(
        ticker: String = "ETHUSDC",
        symbolId: Long = PairsConstants.Navigation.NO_SYMBOL_ID,
    ): ComparisonViewModel =
        ComparisonViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        PairsConstants.Navigation.TICKER_ARG to ticker,
                        PairsConstants.Navigation.SYMBOL_ID_ARG to symbolId,
                    ),
                ),
            comparePairAcrossExchangesUseCase = compare,
            applyComparisonBestPricesUseCase = ApplyComparisonBestPricesUseCase(),
            streamConnectUseCase = connect,
            subscribeSingleTickerUseCase = subscribeSingle,
            restoreTickerSubscriptionsUseCase = restore,
            observeTickerEventUseCase = observeEvents,
            observeStreamReconnectsUseCase = observeReconnects,
        )

    @Test
    fun `init connects and takes over the subscription for the lowercased ticker`() =
        runTest {
            makeVm()
            runCurrent()

            verify(exactly = 1) { connect.invoke() }
            verify(exactly = 1) { subscribeSingle.invoke(TICKER) }
        }

    @Test
    fun `a loaded comparison replaces the spinner`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            val state = vm.uiState.value
            assertFalse(state.loading)
            assertEquals(2, state.comparison!!.quotes.size)
            assertEquals(TICKER, state.ticker)
        }

    @Test
    fun `a failed load shows an error and retry loads again`() =
        runTest {
            coEvery { compare.invoke(TICKER) } returns Result.failure(IllegalStateException("offline"))
            val vm = makeVm()
            runCurrent()

            assertNotNull(vm.uiState.value.error)
            assertNull(vm.uiState.value.comparison)

            coEvery { compare.invoke(TICKER) } returns Result.success(defaultComparison())
            vm.retry()
            runCurrent()

            assertNull(vm.uiState.value.error)
            assertNotNull(vm.uiState.value.comparison)
            coVerify(exactly = 2) { compare.invoke(TICKER) }
        }

    @Test
    fun `a quote tick moves its row only after the interval`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.emit(quoteTick(providerId = 2, ask = 99.0, bid = 98.5))
            runCurrent()

            // до конца интервала строки не двигаются: обновлять UI на каждый тик нельзя
            assertEquals(102.0, vm.row(2).priceSell!!, 0.0)

            advanceInterval()

            assertEquals(99.0, vm.row(2).priceSell!!, 0.0)
            assertEquals(98.5, vm.row(2).priceBuy!!, 0.0)
            assertEquals(101.0, vm.row(1).priceSell!!, 0.0)
        }

    @Test
    fun `live ticks do not reorder the table`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            // вторая биржа стала дешевле первой, но строки остаются на местах —
            // иначе они прыгали бы под пальцем каждые полсекунды
            events.emit(quoteTick(providerId = 2, ask = 90.0, bid = 89.0))
            advanceInterval()

            assertEquals(
                listOf(1, 2),
                vm.uiState.value.comparison!!
                    .quotes
                    .map { it.provider.id },
            )
        }

    @Test
    fun `a zero side in a live tick shows a dash instead of zero`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.emit(quoteTick(providerId = 1, ask = 0.0, bid = 100.8))
            advanceInterval()

            assertNull(vm.row(1).priceSell)
            assertEquals(100.8, vm.row(1).priceBuy!!, 0.0)
        }

    @Test
    fun `events of other tickers are ignored`() =
        runTest {
            val vm = makeVm()
            runCurrent()
            val before = vm.uiState.value.comparison

            events.emit(quoteTick(providerId = 1, ask = 1.0, bid = 0.9, ticker = "btcusdt"))
            events.emit(bestEvent(symbolId = 1, askId = 2, bidId = 2, spread = 3.0, ticker = "btcusdt"))
            advanceInterval()

            assertEquals(before, vm.uiState.value.comparison)
        }

    @Test
    fun `rapid ticks land as one state update with the latest prices`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            val comparisons = mutableListOf<PairComparison?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.uiState.collect { comparisons += it.comparison }
            }
            val initialUpdates = comparisons.size

            repeat(RAPID_TICKS) { index ->
                events.emit(quoteTick(providerId = 1, ask = 101.0 + index, bid = 100.0 + index))
                events.emit(quoteTick(providerId = 2, ask = 202.0 + index, bid = 201.0 + index))
            }
            advanceInterval()

            assertEquals(initialUpdates + 1, comparisons.size)
            assertEquals(101.0 + RAPID_TICKS - 1, vm.row(1).priceSell!!, 0.0)
            assertEquals(201.0 + RAPID_TICKS - 1, vm.row(2).priceBuy!!, 0.0)
        }

    @Test
    fun `the flush resumes after a quiet interval`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.emit(quoteTick(providerId = 1, ask = 105.0, bid = 104.0))
            advanceInterval()
            // пустой интервал: джоб выходит и должен подняться заново на следующем тике
            advanceInterval()

            events.emit(quoteTick(providerId = 1, ask = 106.0, bid = 105.0))
            advanceInterval()

            assertEquals(106.0, vm.row(1).priceSell!!, 0.0)
        }

    @Test
    fun `a best price event moves the highlight`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            events.emit(bestEvent(symbolId = MAIN_SYMBOL, askId = 2, bidId = 1, spread = 0.4))
            advanceInterval()

            val comparison = vm.uiState.value.comparison!!
            assertEquals(2, comparison.bestAskProviderId)
            assertEquals(1, comparison.bestBidProviderId)
            assertEquals(0.4, comparison.spreadPercent!!, 1e-9)
        }

    @Test
    fun `a narrower symbol of the same ticker does not steal the summary`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            // оба события в одном интервале: раньше хранилось одно на тикер,
            // и выжимку забирал тот символ, что пришёл последним
            events.emit(bestEvent(symbolId = MAIN_SYMBOL, askId = 1, bidId = 2, spread = 0.2))
            events.emit(bestEvent(symbolId = OTHER_SYMBOL, askId = 2, bidId = 2, spread = -0.008))
            advanceInterval()

            val comparison = vm.uiState.value.comparison!!
            assertEquals(1, comparison.bestAskProviderId)
            assertEquals(0.2, comparison.spreadPercent!!, 1e-9)
        }

    @Test
    fun `a reconnect rebuilds the comparison without a spinner`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            val fresh = defaultComparison().copy(spreadPercent = 0.33)
            coEvery { compare.invoke(TICKER) } returns Result.success(fresh)
            reconnects.emit(Unit)
            runCurrent()

            // тики за время разрыва потеряны — сравнение собирается заново
            assertEquals(
                0.33,
                vm.uiState.value.comparison!!
                    .spreadPercent!!,
                0.0,
            )
            assertFalse(vm.uiState.value.loading)
        }

    @Test
    fun `a failed rebuild after a reconnect keeps the table on screen`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            coEvery { compare.invoke(TICKER) } returns Result.failure(IllegalStateException("500"))
            reconnects.emit(Unit)
            runCurrent()

            assertNotNull(vm.uiState.value.comparison)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `a symbol screen ignores events of the ticker's other networks`() =
        runTest {
            coEvery { compare.invoke(TICKER, MAIN_SYMBOL) } returns Result.success(defaultComparison())
            val vm = makeVm(symbolId = MAIN_SYMBOL)
            runCurrent()
            val before = vm.uiState.value.comparison

            // подписка в сокете по тикеру: события символа 14487 приходят сюда же
            events.emit(bestEvent(symbolId = OTHER_SYMBOL, askId = 2, bidId = 2, spread = 5.0))
            advanceInterval()

            assertEquals(before, vm.uiState.value.comparison)
            coVerify { compare.invoke(TICKER, MAIN_SYMBOL) }
        }

    @Test
    fun `clearing the viewmodel ends the subscription takeover`() =
        runTest {
            val vm = makeVm()
            runCurrent()

            clearViewModel(vm)

            verify(exactly = 1) { restore.invoke() }
        }

    @Test
    fun `a blank ticker neither subscribes nor releases someone else's takeover`() =
        runTest {
            val vm = makeVm(ticker = "")
            runCurrent()

            clearViewModel(vm)

            // отпусти он чужой захват — счётчик уехал бы в минус, и каталог
            // вернулся бы в соединение поверх открытого экрана деталей
            verify(exactly = 0) { subscribeSingle.invoke(any()) }
            verify(exactly = 0) { restore.invoke() }
            assertFalse(vm.uiState.value.loading)
        }

    private fun TestScope.advanceInterval() {
        advanceTimeBy(PairsConstants.ComparisonScreen.LIVE_PRICE_INTERVAL_MS + 1)
        runCurrent()
    }

    private fun ComparisonViewModel.row(providerId: Int): ProviderDetail =
        uiState.value.comparison!!
            .quotes
            .single { it.provider.id == providerId }

    private fun defaultComparison(): PairComparison {
        val best = best(symbolId = MAIN_SYMBOL, askId = 1, bidId = 2, spread = -0.09)

        return PairComparison(
            ticker = TICKER,
            quotes =
                listOf(
                    quote(id = 1, ask = 101.0, bid = 100.9),
                    quote(id = 2, ask = 102.0, bid = 101.9),
                ),
            bestAskProviderId = best.bestAskProviderId,
            bestAskPrice = best.bestAskPrice,
            bestBidProviderId = best.bestBidProviderId,
            bestBidPrice = best.bestBidPrice,
            spreadPercent = best.spreadPercent,
            bestPrices = listOf(best),
        )
    }

    private fun quote(
        id: Int,
        ask: Double?,
        bid: Double?,
    ) = ProviderDetail(
        provider = Provider(id = id, name = "exchange$id", referralUrl = null, status = ProviderStatus.Enabled),
        priceSell = ask,
        priceBuy = bid,
    )

    private fun quoteTick(
        providerId: Int,
        ask: Double,
        bid: Double,
        ticker: String = TICKER,
    ) = TickerStreamEvent.TickerPriceChange(
        id = "evt",
        data =
            TickerPrice(
                ticker = ticker,
                symbolId = MAIN_SYMBOL.toInt(),
                providerId = providerId,
                priceSell = ask,
                priceBuy = bid,
            ),
    )

    private fun bestEvent(
        symbolId: Long,
        askId: Int,
        bidId: Int,
        spread: Double,
        ticker: String = TICKER,
    ) = TickerStreamEvent.TickerBestPriceChange(
        id = "evt",
        data = best(symbolId = symbolId, askId = askId, bidId = bidId, spread = spread, ticker = ticker),
    )

    private fun best(
        symbolId: Long,
        askId: Int,
        bidId: Int,
        spread: Double,
        ticker: String = TICKER,
    ) = TickerBestPrice(
        ticker = ticker,
        symbolId = symbolId,
        bestAskProviderId = askId,
        bestAskPrice = 101.0,
        bestBidProviderId = bidId,
        bestBidPrice = 100.9,
        spreadPercent = spread,
    )

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
        provider[ComparisonViewModel::class.java]
        store.clear()
    }

    private companion object {
        const val TICKER = "ethusdc"
        const val MAIN_SYMBOL = 143L
        const val OTHER_SYMBOL = 14487L
        const val RAPID_TICKS = 100
    }
}
