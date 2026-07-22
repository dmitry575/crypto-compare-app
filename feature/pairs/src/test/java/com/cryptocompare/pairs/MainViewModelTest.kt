package com.cryptocompare.pairs

import androidx.paging.PagingData
import com.cryptocompare.domain.usecase.pairs.ApplyTickerPriceChangesUseCase
import com.cryptocompare.domain.usecase.pairs.LoadPairsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveFavouriteTickersUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.StreamDisconnectUseCase
import com.cryptocompare.domain.usecase.pairs.SyncFavouriteTickersUseCase
import com.cryptocompare.domain.usecase.pairs.SyncVisibleTickersUseCase
import com.cryptocompare.domain.usecase.pairs.ToggleFavouriteTickerUseCase
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.viewmodel.mainViewModel.MainViewModel
import com.cryptocompare.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun loadPairsUseCaseMock(
        pagingFlow: Flow<PagingData<PairUiItem>> = flowOf(PagingData.empty()),
    ): LoadPairsUseCase =
        mockk {
            every { this@mockk.invoke(any(), any(), any()) } returns pagingFlow
        }

    private fun observeTickerEventUseCaseMock(flow: Flow<TickerStreamEvent>): ObserveTickerEventUseCase =
        mockk { every { this@mockk.invoke() } returns flow }

    private fun syncVisibleTickersUseCaseMock(
        block: (List<String>, Set<String>) -> Set<String>,
    ): SyncVisibleTickersUseCase =
        mockk {
            every { this@mockk.invoke(any(), any()) } answers { block(firstArg(), secondArg()) }
        }

    private fun streamDisconnectUseCaseMock(): StreamDisconnectUseCase =
        mockk { every { this@mockk.invoke() } just runs }

    private fun applyTickerPriceChangesUseCaseMock(
        result: Result<Unit> = Result.success(Unit),
    ): ApplyTickerPriceChangesUseCase = mockk { coEvery { this@mockk.invoke(any()) } returns result }

    private fun observeFavoriteTickersUseCaseMock(flow: Flow<Set<String>>): ObserveFavouriteTickersUseCase =
        mockk { every { this@mockk.invoke() } returns flow }

    private fun toggleFavoriteTickerUseCaseMock(
        result: Result<Boolean> = Result.success(true),
    ): ToggleFavouriteTickerUseCase = mockk { coEvery { this@mockk.invoke(any()) } returns result }

    private fun syncFavoriteTickersUseCaseMock(
        result: Result<Unit> = Result.success(Unit),
    ): SyncFavouriteTickersUseCase = mockk { coEvery { this@mockk.invoke() } returns result }

    private fun makeVm(
        loadPairsUseCase: LoadPairsUseCase = loadPairsUseCaseMock(),
        observeTickerEventUseCase: ObserveTickerEventUseCase = observeTickerEventUseCaseMock(emptyFlow()),
        syncVisibleTickersUseCase: SyncVisibleTickersUseCase =
            syncVisibleTickersUseCaseMock { v, _ ->
                v.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            },
        streamDisconnectUseCase: StreamDisconnectUseCase = streamDisconnectUseCaseMock(),
        applyTickerPriceChangesUseCase: ApplyTickerPriceChangesUseCase = applyTickerPriceChangesUseCaseMock(),
        observeFavouriteTickersUseCase: ObserveFavouriteTickersUseCase =
            observeFavoriteTickersUseCaseMock(flowOf(emptySet())),
        toggleFavouriteTickerUseCase: ToggleFavouriteTickerUseCase = toggleFavoriteTickerUseCaseMock(),
        syncFavouriteTickersUseCase: SyncFavouriteTickersUseCase = syncFavoriteTickersUseCaseMock(),
    ): MainViewModel =
        MainViewModel(
            loadPairsUseCase = loadPairsUseCase,
            syncVisibleTickersUseCase = syncVisibleTickersUseCase,
            streamDisconnectUseCase = streamDisconnectUseCase,
            observeTickerEventUseCase = observeTickerEventUseCase,
            applyTickerPriceChangesUseCase = applyTickerPriceChangesUseCase,
            observeFavouriteTickersUseCase = observeFavouriteTickersUseCase,
            toggleFavouriteTickerUseCase = toggleFavouriteTickerUseCase,
            syncFavouriteTickersUseCase = syncFavouriteTickersUseCase,
        )

    private fun priceChange(
        symbolId: Int,
        priceBuy: Double,
        priceSell: Double,
        ticker: String = "btcusdt",
    ) = TickerStreamEvent.TickerPriceChange(
        id = "evt",
        data =
            TickerPrice(
                ticker = ticker,
                symbolId = symbolId,
                providerId = 1,
                priceSell = priceSell,
                priceBuy = priceBuy,
            ),
    )

    @Test
    fun `collecting pairs invokes load use case with current filters`() =
        runTest {
            val loadPairsUseCase = loadPairsUseCaseMock()
            val vm = makeVm(loadPairsUseCase = loadPairsUseCase)

            val collectJob = launch { vm.pairs.collect {} }
            advanceTimeBy(400)
            runCurrent()

            coVerify { loadPairsUseCase.invoke("", false, emptySet()) }
            collectJob.cancel()
        }

    @Test
    fun `search query change recreates pager with new query`() =
        runTest {
            val loadPairsUseCase = loadPairsUseCaseMock()
            val vm = makeVm(loadPairsUseCase = loadPairsUseCase)

            val collectJob = launch { vm.pairs.collect {} }
            advanceTimeBy(400)
            runCurrent()

            vm.onSearchQueryChange("btc")
            advanceTimeBy(400)
            runCurrent()

            coVerify { loadPairsUseCase.invoke("btc", false, emptySet()) }
            collectJob.cancel()
        }

    @Test
    fun `price events are batched and applied once per flush interval`() =
        runTest {
            val events = MutableSharedFlow<TickerStreamEvent>(extraBufferCapacity = 8)
            val applyUseCase = applyTickerPriceChangesUseCaseMock()
            val vm =
                makeVm(
                    observeTickerEventUseCase = observeTickerEventUseCaseMock(events),
                    applyTickerPriceChangesUseCase = applyUseCase,
                )

            runCurrent()

            events.tryEmit(priceChange(symbolId = 1, priceBuy = 100.0, priceSell = 101.0))
            events.tryEmit(priceChange(symbolId = 1, priceBuy = 110.0, priceSell = 111.0))
            events.tryEmit(priceChange(symbolId = 2, priceBuy = 10.0, priceSell = 11.0, ticker = "ethusdt"))
            runCurrent()

            val batchSlot = slot<List<TickerPrice>>()
            advanceTimeBy(600)
            runCurrent()

            coVerify(exactly = 1) { applyUseCase.invoke(capture(batchSlot)) }
            val batch = batchSlot.captured
            assertEquals(2, batch.size)
            // for the same symbol only the latest tick survives
            assertEquals(110.0, batch.first { it.symbolId == 1 }.priceBuy, 0.0)
            assertEquals(10.0, batch.first { it.symbolId == 2 }.priceBuy, 0.0)

            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `flush loop does nothing when no price events arrived`() =
        runTest {
            val applyUseCase = applyTickerPriceChangesUseCaseMock()
            makeVm(applyTickerPriceChangesUseCase = applyUseCase)

            advanceTimeBy(2000)
            runCurrent()

            coVerify(exactly = 0) { applyUseCase.invoke(any()) }
        }

    @Test
    fun `apply batch failure sets error`() =
        runTest {
            val events = MutableSharedFlow<TickerStreamEvent>(extraBufferCapacity = 8)
            val vm =
                makeVm(
                    observeTickerEventUseCase = observeTickerEventUseCaseMock(events),
                    applyTickerPriceChangesUseCase =
                        applyTickerPriceChangesUseCaseMock(
                            Result.failure(IllegalStateException("db write failed")),
                        ),
                )

            runCurrent()
            events.tryEmit(priceChange(symbolId = 1, priceBuy = 100.0, priceSell = 101.0))
            advanceTimeBy(600)
            runCurrent()

            assertEquals("db write failed", vm.uiState.value.error)
        }

    @Test
    fun `socket flow failure sets error and does not crash`() =
        runTest {
            val vm =
                makeVm(
                    observeTickerEventUseCase =
                        observeTickerEventUseCaseMock(flow { throw IllegalStateException("socket disconnected") }),
                )

            yield()

            assertEquals("socket disconnected", vm.uiState.value.error)
        }

    @Test
    fun `onVisibleTickersChange updates subscribed tickers from use case result`() =
        runTest {
            val vm = makeVm()

            yield()
            vm.onVisibleTickersChange(listOf("BTCUSDT", "ethusdt", ""))

            assertEquals(setOf("btcusdt", "ethusdt"), vm.uiState.value.subscribedTickers)
        }

    @Test
    fun `onVisibleTickersChange uses latest use case result`() =
        runTest {
            val vm = makeVm()

            yield()
            vm.onVisibleTickersChange(listOf("BTCUSDT", "ethusdt"))
            vm.onVisibleTickersChange(listOf("ETHUSDT"))

            assertEquals(setOf("ethusdt"), vm.uiState.value.subscribedTickers)
        }

    @Test
    fun `init observes favourites and updates favouriteTickers in uiState`() =
        runTest {
            val vm =
                makeVm(
                    observeFavouriteTickersUseCase =
                        observeFavoriteTickersUseCaseMock(flowOf(setOf("BTCUSDT", "ETHUSDT"))),
                )

            yield()

            assertEquals(setOf("BTCUSDT", "ETHUSDT"), vm.uiState.value.favouriteTickers)
        }

    @Test
    fun `favouriteTickers updates when flow emits new set`() =
        runTest {
            val favouritesFlow = MutableSharedFlow<Set<String>>(extraBufferCapacity = 1)
            val vm =
                makeVm(
                    observeFavouriteTickersUseCase = observeFavoriteTickersUseCaseMock(favouritesFlow),
                )

            yield()
            assertEquals(emptySet<String>(), vm.uiState.value.favouriteTickers)

            favouritesFlow.emit(setOf("BTCUSDT"))
            yield()
            assertEquals(setOf("BTCUSDT"), vm.uiState.value.favouriteTickers)

            favouritesFlow.emit(setOf("BTCUSDT", "ETHUSDT"))
            yield()
            assertEquals(setOf("BTCUSDT", "ETHUSDT"), vm.uiState.value.favouriteTickers)
        }

    @Test
    fun `onFavouriteClick success does not set error`() =
        runTest {
            val vm =
                makeVm(
                    toggleFavouriteTickerUseCase = toggleFavoriteTickerUseCaseMock(Result.success(true)),
                )

            yield()
            vm.onFavouriteClick("BTCUSDT")
            yield()

            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `onFavouriteClick failure sets error message`() =
        runTest {
            val vm =
                makeVm(
                    toggleFavouriteTickerUseCase =
                        toggleFavoriteTickerUseCaseMock(Result.failure(IllegalStateException("toggle failed"))),
                )

            yield()
            vm.onFavouriteClick("BTCUSDT")
            yield()

            assertEquals("toggle failed", vm.uiState.value.error)
        }

    @Test
    fun `onFavouriteClick failure with null message falls back to default error`() =
        runTest {
            val vm =
                makeVm(
                    toggleFavouriteTickerUseCase =
                        toggleFavoriteTickerUseCaseMock(Result.failure(IllegalStateException(null as String?))),
                )

            yield()
            vm.onFavouriteClick("BTCUSDT")
            yield()

            assertEquals("Favourite toggle error", vm.uiState.value.error)
        }

    @Test
    fun `onOnlyFavouriteChange true sets onlyFavourite flag`() =
        runTest {
            val vm = makeVm()

            vm.onOnlyFavouriteChange(true)

            assertEquals(true, vm.uiState.value.onlyFavourite)
        }

    @Test
    fun `onOnlyFavouriteChange false clears onlyFavourite flag`() =
        runTest {
            val vm = makeVm()

            vm.onOnlyFavouriteChange(true)
            vm.onOnlyFavouriteChange(false)

            assertEquals(false, vm.uiState.value.onlyFavourite)
        }

    @Test
    fun `onErrorShown clears error`() =
        runTest {
            val vm =
                makeVm(
                    toggleFavouriteTickerUseCase =
                        toggleFavoriteTickerUseCaseMock(Result.failure(IllegalStateException("toggle failed"))),
                )

            yield()
            vm.onFavouriteClick("BTCUSDT")
            yield()
            assertEquals("toggle failed", vm.uiState.value.error)

            vm.onErrorShown()

            assertNull(vm.uiState.value.error)
        }

    @Test
    fun `syncFavouriteTickers failure sets error`() =
        runTest {
            val vm =
                makeVm(
                    syncFavouriteTickersUseCase =
                        syncFavoriteTickersUseCaseMock(Result.failure(IllegalStateException("sync failed"))),
                )

            yield()

            assertEquals("sync failed", vm.uiState.value.error)
        }
}
