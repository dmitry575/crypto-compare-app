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
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.ChartWindow
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.updateLastCandle
import com.cryptocompare.pairs.util.withLivePrices
import com.cryptocompare.pairs.util.withNewerPage
import com.cryptocompare.pairs.util.withOlderPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

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

        /**
         * Уже загруженные графики по паре (биржа + масштаб). График привязан к
         * выбранной бирже, поэтому ключ — пара providerId+timeframe; повторный
         * заход на тот же ключ не идёт в сеть. Кеша на диске нет — истории много.
         */
        private val chartsByKey = mutableMapOf<ChartKey, ChartWindow>()

        /** Захватили ли подписку под этот экран — чтобы отпустить её ровно один раз. */
        private var subscriptionTakenOver = false

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
            subscribeSingleTickerUseCase(ticker)
            subscriptionTakenOver = true

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
                    delay(PairsConstants.DetailScreen.LIVE_PRICE_INTERVAL_MS.milliseconds)

                    val tick =
                        synchronized(pendingTickLock) {
                            val current = pendingTick
                            if (current == null) isTickFlushScheduled = false
                            pendingTick = null
                            current
                        } ?: return@launch
                    applyLiveTick(tick)
                }
            }
        }

        /** Один тик двигает цены карточек бирж, а последний бар — только у своей биржи. */
        private fun applyLiveTick(tick: TickerPrice) {
            val state = _uiState.value
            // цены карточек двигаем всегда: у каждой биржи свой тик
            val exchanges = state.exchanges.withLivePrices(tick)

            // последний бар графика двигает только тик выбранной биржи — по ней он
            // и построен — и только пока окно стоит на живом крае (newestSkip == 0)
            val selectedProviderId = state.selectedExchange?.provider?.id
            val key = selectedProviderId?.let { ChartKey(it, state.timeframe) }
            val chart = key?.let { chartsByKey[it] }
            if (key != null && chart != null && tick.providerId == selectedProviderId && chart.newestSkip == 0) {
                // цена бара — середина спреда: priceSell это ask, priceBuy это bid
                val midPrice = (tick.priceSell + tick.priceBuy) / 2.0
                val updated =
                    updateLastCandle(
                        candles = chart.candles,
                        price = midPrice,
                        timeframe = state.timeframe,
                        nowMillis = System.currentTimeMillis(),
                    )
                // тик мог открыть новый бар — он живой, не серверный: учитываем в liveCount
                chartsByKey[key] =
                    chart.copy(candles = updated, liveCount = chart.liveCount + (updated.size - chart.candles.size))
                _uiState.update { it.copy(candles = updated, exchanges = exchanges) }
            } else {
                _uiState.update { it.copy(exchanges = exchanges) }
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
                            // график строится по выбранной бирже (по умолчанию — первой в списке)
                            _uiState.value.selectedExchange?.provider?.id?.let { providerId ->
                                loadInitialCandles(providerId, ticker, _uiState.value.timeframe)
                            }
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
            _uiState.update { it.copy(timeframe = timeframe) }
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return
            loadInitialCandles(providerId, _uiState.value.ticker, timeframe)
        }

        fun onExchangeSelected(index: Int) {
            if (_uiState.value.selectedExchangeIndex == index) return
            _uiState.update { it.copy(selectedExchangeIndex = index) }
            // график привязан к бирже: при смене перегружаем (из кеша, если уже был)
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return
            loadInitialCandles(providerId, _uiState.value.ticker, _uiState.value.timeframe)
        }

        /** Первая страница графика для (биржа, масштаб): из кеша либо самые свежие свечи. */
        private fun loadInitialCandles(
            providerId: Int,
            symbol: String,
            timeframe: ChartTimeframe,
        ) {
            val key = ChartKey(providerId, timeframe)
            chartsByKey[key]?.let { cached ->
                emitChart(cached, loading = false)
                return
            }

            _uiState.update {
                it.copy(
                    candles = emptyList(),
                    chartLoading = true,
                    chartLoadingMore = false,
                    chartCanLoadOlder = false,
                    chartCanLoadNewer = false,
                )
            }
            viewModelScope.launch {
                getTickerHistoryUseCase(providerId, symbol, timeframe, PairsConstants.Chart.PAGE_LIMIT, offset = 0)
                    .onSuccess { candles ->
                        val window = ChartWindow.initial(candles, PairsConstants.Chart.WINDOW_MAX_CANDLES)
                        chartsByKey[key] = window
                        if (isCurrentChart(providerId, timeframe)) emitChart(window, loading = false)
                    }.onFailure { error ->
                        // сбой графика не ломает экран: цены и биржи остаются доступны
                        if (error is CancellationException) throw error
                        if (isCurrentChart(providerId, timeframe)) _uiState.update { it.copy(chartLoading = false) }
                    }
            }
        }

        /**
         * Догрузка более старой страницы (левый край окна) — вызывается графиком при
         * прокрутке к левому краю. Свечи прибавляются слева, свежий край при
         * переполнении окна выгружается; позицию сохраняет сам график.
         */
        fun loadOlderCandles() {
            val key = currentChartKey() ?: return
            val chart = chartsByKey[key] ?: return
            if (!canPage(chart.canLoadOlder)) return

            _uiState.update { it.copy(chartLoadingMore = true) }
            viewModelScope.launch {
                getTickerHistoryUseCase(
                    key.providerId,
                    _uiState.value.ticker,
                    key.timeframe,
                    PairsConstants.Chart.PAGE_LIMIT,
                    offset = chart.oldestSkip,
                ).onSuccess { older ->
                    val current = chartsByKey[key] ?: return@onSuccess
                    val updated = current.withOlderPage(older, PairsConstants.Chart.WINDOW_MAX_CANDLES)
                    chartsByKey[key] = updated
                    if (isCurrentChart(key.providerId, key.timeframe)) emitChart(updated, loading = false)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    if (isCurrentChart(
                            key.providerId,
                            key.timeframe,
                        )
                    ) {
                        _uiState.update { it.copy(chartLoadingMore = false) }
                    }
                }
            }
        }

        /**
         * Догрузка более свежей страницы (правый край окна) — когда свежий край был
         * подрезан и пользователь листает обратно к настоящему. Свечи прибавляются
         * справа, старый край при переполнении окна выгружается.
         */
        fun loadNewerCandles() {
            val key = currentChartKey() ?: return
            val chart = chartsByKey[key] ?: return
            if (!canPage(chart.canLoadNewer)) return

            val offset = (chart.newestSkip - PairsConstants.Chart.PAGE_LIMIT).coerceAtLeast(0)
            val limit = chart.newestSkip - offset

            _uiState.update { it.copy(chartLoadingMore = true) }
            viewModelScope.launch {
                getTickerHistoryUseCase(key.providerId, _uiState.value.ticker, key.timeframe, limit, offset)
                    .onSuccess { newer ->
                        val current = chartsByKey[key] ?: return@onSuccess
                        val updated = current.withNewerPage(newer, offset, PairsConstants.Chart.WINDOW_MAX_CANDLES)
                        chartsByKey[key] = updated
                        if (isCurrentChart(key.providerId, key.timeframe)) emitChart(updated, loading = false)
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        if (isCurrentChart(
                                key.providerId,
                                key.timeframe,
                            )
                        ) {
                            _uiState.update { it.copy(chartLoadingMore = false) }
                        }
                    }
            }
        }

        private fun canPage(sideAllowed: Boolean): Boolean {
            val state = _uiState.value
            return sideAllowed && !state.chartLoading && !state.chartLoadingMore
        }

        private fun currentChartKey(): ChartKey? {
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return null
            return ChartKey(providerId, _uiState.value.timeframe)
        }

        private fun emitChart(
            window: ChartWindow,
            loading: Boolean,
        ) {
            _uiState.update {
                it.copy(
                    candles = window.candles,
                    chartLoading = loading,
                    chartLoadingMore = false,
                    chartCanLoadOlder = window.canLoadOlder,
                    chartCanLoadNewer = window.canLoadNewer,
                )
            }
        }

        // пока страница ехала, пользователь мог сменить биржу или масштаб — тогда
        // результат уже не относится к тому, что на экране, и в state его не льём
        private fun isCurrentChart(
            providerId: Int,
            timeframe: ChartTimeframe,
        ): Boolean {
            val state = _uiState.value
            return state.selectedExchange?.provider?.id == providerId && state.timeframe == timeframe
        }

        private data class ChartKey(
            val providerId: Int,
            val timeframe: ChartTimeframe,
        )

        override fun onCleared() {
            // отпускаем захват только если сами его брали — иначе чужой счётчик уедет
            // в минус и каталог вернётся раньше времени поверх активного экрана
            if (subscriptionTakenOver) {
                restoreTickerSubscriptionsUseCase()
            }
            super.onCleared()
        }
    }
