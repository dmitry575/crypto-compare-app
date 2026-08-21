package com.cryptocompare.pairs.viewmodel.mainViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cryptocompare.domain.usecase.pairs.ApplyTickerPriceChangesUseCase
import com.cryptocompare.domain.usecase.pairs.LoadPairsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveFavouriteTickersUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.StreamDisconnectUseCase
import com.cryptocompare.domain.usecase.pairs.SyncFavouriteTickersUseCase
import com.cryptocompare.domain.usecase.pairs.SyncVisibleTickersUseCase
import com.cryptocompare.domain.usecase.pairs.ToggleFavouriteTickerUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.PairsConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val loadPairsUseCase: LoadPairsUseCase,
        private val syncVisibleTickersUseCase: SyncVisibleTickersUseCase,
        private val streamDisconnectUseCase: StreamDisconnectUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
        private val applyTickerPriceChangesUseCase: ApplyTickerPriceChangesUseCase,
        private val observeFavouriteTickersUseCase: ObserveFavouriteTickersUseCase,
        private val syncFavouriteTickersUseCase: SyncFavouriteTickersUseCase,
        private val toggleFavouriteTickerUseCase: ToggleFavouriteTickerUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState = _uiState.asStateFlow()

        private val subscribedTickers = mutableSetOf<String>()

        // price updates from the socket are accumulated here and flushed to the
        // database in batches, so the UI is not re-rendered on every single tick
        private val pendingPriceUpdates = mutableMapOf<Int, TickerPrice>()
        private val pendingPricesLock = Any()

        // guarded by pendingPricesLock; cleared in the SAME critical section that
        // observes an empty queue, so a tick can't slip in between "queue is empty"
        // and the job ending and get stranded with no flush scheduled for it
        private var isFlushScheduled = false

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val pairs: Flow<PagingData<PairUiItem>> =
            combine(
                _uiState
                    .map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce { query -> if (query.isEmpty()) 0L else PairsConstants.MainScreen.SEARCH_DEBOUNCE_MS },
                _uiState.map { it.onlyFavourite }.distinctUntilChanged(),
                _uiState.map { it.favouriteTickers }.distinctUntilChanged(),
            ) { query, onlyFavourite, favourites ->
                PairsFilter(
                    query = query,
                    onlyFavourite = onlyFavourite,
                    // the favourites set matters for the query only when the filter is
                    // on; dropping it otherwise keeps star taps from rebuilding the pager
                    favouriteTickers = if (onlyFavourite) favourites else emptySet(),
                )
            }.distinctUntilChanged()
                .flatMapLatest { filter ->
                    loadPairsUseCase(
                        query = filter.query,
                        onlyFavourite = filter.onlyFavourite,
                        favouriteTickers = filter.favouriteTickers,
                    )
                }.cachedIn(viewModelScope)

        init {
            observeSocket()
            syncFavouriteTickers()
            observeFavouriteTickers()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
        }

        fun onFavouriteClick(ticker: String) {
            viewModelScope.launch {
                toggleFavouriteTickerUseCase(ticker).onFailure { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Favourite toggle error") }
                }
            }
        }

        fun onOnlyFavouriteChange(enabled: Boolean) {
            _uiState.update { it.copy(onlyFavourite = enabled) }
        }

        fun onVisibleTickersChange(visibleTickers: List<String>) {
            val updatedSubscribedTickers = syncVisibleTickersUseCase(visibleTickers, subscribedTickers)

            subscribedTickers.clear()
            subscribedTickers.addAll(updatedSubscribedTickers)

            _uiState.update { it.copy(subscribedTickers = updatedSubscribedTickers) }
        }

        fun onErrorShown() {
            _uiState.update { it.copy(error = null) }
        }

        private fun observeSocket() {
            viewModelScope.launch {
                try {
                    observeTickerEventUseCase().collect { event ->
                        if (event is TickerStreamEvent.TickerPriceChange) {
                            synchronized(pendingPricesLock) {
                                pendingPriceUpdates[event.data.symbolId] = event.data
                            }
                            schedulePriceFlush()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.toUserMessage()) }
                }
            }
        }

        // the flush job lives only while ticks keep coming: one interval with no
        // new updates and it exits, the next tick schedules it again
        private fun schedulePriceFlush() {
            synchronized(pendingPricesLock) {
                if (isFlushScheduled) return
                isFlushScheduled = true
            }

            viewModelScope.launch {
                while (true) {
                    delay(PairsConstants.MainScreen.PRICE_FLUSH_INTERVAL_MS)

                    val batch =
                        synchronized(pendingPricesLock) {
                            // клеим флаг к наблюдению пустоты: тик, добавленный
                            // до этого блока, попадёт в batch; добавленный после —
                            // увидит isFlushScheduled == false и запустит новый джоб
                            if (pendingPriceUpdates.isEmpty()) {
                                isFlushScheduled = false
                                return@launch
                            }
                            pendingPriceUpdates.values.toList().also { pendingPriceUpdates.clear() }
                        }

                    applyTickerPriceChangesUseCase(batch).onFailure { exception ->
                        _uiState.update { it.copy(error = exception.toUserMessage()) }
                    }
                }
            }
        }

        fun syncFavouriteTickers() {
            viewModelScope.launch {
                syncFavouriteTickersUseCase().onFailure { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Couldn't sync favourite tickers") }
                }
            }
        }

        private fun observeFavouriteTickers() {
            viewModelScope.launch {
                observeFavouriteTickersUseCase().collect { favourites ->
                    _uiState.update { it.copy(favouriteTickers = favourites) }
                }
            }
        }

        override fun onCleared() {
            streamDisconnectUseCase()
            super.onCleared()
        }

        private data class PairsFilter(
            val query: String,
            val onlyFavourite: Boolean,
            val favouriteTickers: Set<String>,
        )
    }
