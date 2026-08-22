package com.cryptocompare.pairs.viewmodel.detailViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.pairs.GetTickerDetailUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerHistoryUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.SubscribeSingleTickerUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.updateLastCandle
import com.cryptocompare.pairs.util.withLivePrices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class DetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getPairDetailsUseCase: GetTickerDetailUseCase,
        private val getTickerHistoryUseCase: GetTickerHistoryUseCase,
        private val streamConnectUseCase: StreamConnectUseCase,
        private val subscribeSingleTickerUseCase: SubscribeSingleTickerUseCase,
        private val restoreTickerSubscriptionsUseCase: RestoreTickerSubscriptionsUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DetailUiState())
        val uiState = _uiState.asStateFlow()

        /** Загруженные масштабы: повторное переключение не идёт в сеть. */
        private val candlesByTimeframe = mutableMapOf<ChartTimeframe, List<Candle>>()

        /** Подписки каталога, снятые на время показа деталей: слотов у соединения мало. */
        private var previousSubscriptions: Set<String> = emptySet()

        private val pendingTickLock = Any()

        @Volatile private var isTickFlushScheduled = false

        private var pendingTick: TickerPrice? = null

        init {
            val ticker = savedStateHandle.get<String>(PairsConstants.Navigation.TICKER_ARG)?.lowercase() ?: ""
            _uiState.update { it.copy(ticker = ticker) }
            loadPairDetails(ticker)
            observeLivePrice(ticker)
        }

        /**
         * Живой последний бар графика. Соединение может быть закрыто после ухода
         * с каталога, поэтому сначала connect(): он идемпотентен. Подписка одна на
         * весь экран — тики фильтруются по тикеру пары.
         */
        private fun observeLivePrice(ticker: String) {
            if (ticker.isBlank()) return

            streamConnectUseCase()
            previousSubscriptions = subscribeSingleTickerUseCase(ticker)

            viewModelScope.launch {
                try {
                    observeTickerEventUseCase().collect { event ->
                        if (event is TickerStreamEvent.TickerPriceChange && event.data.ticker == ticker) {
                            synchronized(pendingTickLock) {
                                pendingTick = event.data
                            }
                            scheduleTickFlush()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                }
            }
        }

        /**
         * Тики приходят десятками в секунду, а состояние UI обновляется раз в
         * интервал последним накопленным значением — тот же батч-паттерн, что и у
         * цен каталога. Цикл завершается, как только тики перестают приходить.
         */
        private fun scheduleTickFlush() {
            synchronized(pendingTickLock) {
                if (isTickFlushScheduled) return
                isTickFlushScheduled = true
            }

            viewModelScope.launch {
                while (true) {
                    delay(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS)

                    val tick =
                        synchronized(pendingTickLock) {
                            val current = pendingTick
                            pendingTick = null
                            current
                        } ?: run {
                            isTickFlushScheduled = false
                            return@launch
                        }

                    applyLiveTick(tick)
                }
            }
        }

        /** Один тик двигает и последний бар графика, и цены карточек бирж. */
        private fun applyLiveTick(tick: TickerPrice) {
            // цена бара — середина спреда: priceSell это ask, priceBuy это bid
            val midPrice = (tick.priceSell + tick.priceBuy) / 2.0
            val timeframe = _uiState.value.timeframe
            val updatedCandles =
                updateLastCandle(
                    candles = _uiState.value.candles,
                    price = midPrice,
                    timeframe = timeframe,
                    nowMillis = System.currentTimeMillis(),
                )

            candlesByTimeframe[timeframe] = updatedCandles
            _uiState.update { state ->
                state.copy(
                    candles = updatedCandles,
                    exchanges = state.exchanges.withLivePrices(tick),
                )
            }
        }

        private fun loadPairDetails(ticker: String) {
            _uiState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                try {
                    val result = getPairDetailsUseCase(ticker)
                    result.fold(
                        onSuccess = { details ->
                            _uiState.update { state ->
                                state.copy(
                                    loading = false,
                                    exchanges = details.exchanges,
                                    selectedExchangeIndex = 0,
                                )
                            }
                            // один график на экран: не зависит от выбранной биржи,
                            // поэтому и грузится ровно один раз на масштаб
                            loadCandles(ticker, _uiState.value.timeframe)
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(loading = false, error = error.toUserMessage()) }
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(loading = false, error = e.toUserMessage()) }
                }
            }
        }

        fun onTimeframeSelected(timeframe: ChartTimeframe) {
            if (_uiState.value.timeframe == timeframe) return

            val cached = candlesByTimeframe[timeframe]
            _uiState.update { it.copy(timeframe = timeframe, candles = cached.orEmpty()) }

            // уже загруженный масштаб не перезапрашиваем — экономим лимит API
            if (cached == null) {
                loadCandles(_uiState.value.ticker, timeframe)
            }
        }

        private fun loadCandles(
            ticker: String,
            timeframe: ChartTimeframe,
        ) {
            _uiState.update { it.copy(chartLoading = true) }
            viewModelScope.launch {
                getTickerHistoryUseCase(ticker, timeframe)
                    .onSuccess { candles ->
                        candlesByTimeframe[timeframe] = candles
                        _uiState.update { state ->
                            // пока грузили, пользователь мог переключить масштаб
                            if (state.timeframe == timeframe) {
                                state.copy(candles = candles, chartLoading = false)
                            } else {
                                state.copy(chartLoading = false)
                            }
                        }
                    }.onFailure { error ->
                        // сбой графика не ломает экран: цены и биржи остаются доступны
                        if (error is CancellationException) throw error
                        _uiState.update { it.copy(chartLoading = false) }
                    }
            }
        }

        fun onExchangeSelected(index: Int) {
            if (_uiState.value.selectedExchangeIndex == index) return
            _uiState.update { it.copy(selectedExchangeIndex = index) }
            // Больше не перезагружаем историю
        }

        override fun onCleared() {
            // каталог ждёт свои подписки обратно, даже если его композиция ещё жива
            restoreTickerSubscriptionsUseCase(previousSubscriptions)
            super.onCleared()
        }
    }
