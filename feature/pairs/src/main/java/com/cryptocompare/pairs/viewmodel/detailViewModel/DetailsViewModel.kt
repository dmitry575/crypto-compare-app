package com.cryptocompare.pairs.viewmodel.detailViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.pairs.GetBestPricesUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerDetailUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerHistoryUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveStreamReconnectsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.TakeOverTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.settings.GetMarketPreferencesUseCase
import com.cryptocompare.domain.usecase.settings.SetChartIndicatorsUseCase
import com.cryptocompare.helpers.withUpdates
import com.cryptocompare.model.chart.ChartIndicator
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.model.error.asAppError
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.settings.MarketPreferences
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.ChartHistory
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.putRecent
import com.cryptocompare.pairs.util.updateLastCandle
import com.cryptocompare.pairs.util.withLiveCandles
import com.cryptocompare.pairs.util.withLivePrices
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
        private val takeOverTickerSubscriptionsUseCase: TakeOverTickerSubscriptionsUseCase,
        private val restoreTickerSubscriptionsUseCase: RestoreTickerSubscriptionsUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
        private val observeStreamReconnectsUseCase: ObserveStreamReconnectsUseCase,
        private val getBestPricesUseCase: GetBestPricesUseCase,
        private val getMarketPreferencesUseCase: GetMarketPreferencesUseCase,
        private val setChartIndicatorsUseCase: SetChartIndicatorsUseCase,
    ) : ViewModel() {
        /** Символ пары: биржи, лучшая пара и живые события берутся только его. */
        private val symbolId: Long? =
            savedStateHandle
                .get<Long>(PairsConstants.Navigation.SYMBOL_ID_ARG)
                ?.takeIf { it != PairsConstants.Navigation.NO_SYMBOL_ID }

        private val _uiState = MutableStateFlow(DetailUiState(symbolId = symbolId))
        val uiState = _uiState.asStateFlow()

        /**
         * Уже загруженные графики по паре (биржа + масштаб). График привязан к
         * выбранной бирже, поэтому ключ — пара providerId+timeframe; повторный
         * заход на тот же ключ не идёт в сеть. Кеша на диске нет — истории много.
         *
         * Держим только последние [PairsConstants.Chart.MAX_CACHED_CHARTS]: бирж у
         * пары бывает два десятка, масштабов пять, и каждая история растёт до
         * [PairsConstants.Chart.MAX_CANDLES]. Без предела перебор бирж на одном
         * экране копил бы сотни тысяч свечей, к которым пользователь не вернётся.
         * Пишем через `putRecent`: открытый график трогается на каждом тике и не
         * вытесняется никогда.
         */
        private val chartsByKey = mutableMapOf<ChartKey, ChartHistory>()

        /** Захватили ли подписку под этот экран — чтобы отпустить её ровно один раз. */
        private var subscriptionTakenOver = false

        private val pendingTickLock = Any()

        @Volatile private var isTickFlushScheduled = false

        /**
         * Последний тик каждой биржи за интервал. Раньше здесь была одна переменная
         * на весь экран, и из тиков разных бирж за полсекунды доживал только
         * последний: карточки остальных бирж стояли, пока им не повезёт тикнуть
         * последними.
         */
        private val pendingTicks = mutableMapOf<Int, TickerPrice>()

        /** Лучшие пары за интервал, по символу: у тикера их бывает несколько. */
        private val pendingBestPrices = mutableMapOf<Long, TickerBestPrice>()

        init {
            val ticker = savedStateHandle.get<String>(PairsConstants.Navigation.TICKER_ARG)?.lowercase() ?: ""
            _uiState.update { it.copy(ticker = ticker) }
            loadPairDetails(ticker)
            loadBestPrices(ticker)
            observeLivePrice(ticker)
            observeReconnects(ticker)
        }

        /**
         * Снекбар показал ошибку — она своё отработала. Без сброса та же ошибка
         * второй раз не показалась бы (состояние не поменялось бы), а после
         * поворота экрана старый снекбар всплыл бы снова.
         */
        fun onErrorShown() {
            _uiState.update { it.copy(error = null) }
        }

        /** «Повторить» после неудачной загрузки: биржи и лучшая пара берутся заново. */
        fun retry() {
            val ticker = _uiState.value.ticker
            loadPairDetails(ticker)
            loadBestPrices(ticker)
        }

        /**
         * После реконнекта цены бирж берутся заново: тики за время разрыва потеряны.
         * Без спиннера и без сброса выбранной биржи — пользователь смотрит на экран.
         * График не перезагружается: его кадр считается от свечей, загруженных при
         * открытии, и замена истории сдвинула бы его под пальцем.
         */
        private fun observeReconnects(ticker: String) {
            if (ticker.isBlank()) return

            viewModelScope.launch {
                observeStreamReconnectsUseCase().collect {
                    loadBestPrices(ticker)
                    getPairDetailsUseCase(ticker, symbolId).onSuccess { details ->
                        _uiState.update { state ->
                            val selectedId = state.selectedExchange?.provider?.id
                            val selectedIndex =
                                details.exchanges
                                    .indexOfFirst { it.provider.id == selectedId }
                                    .takeIf { it >= 0 } ?: state.selectedExchangeIndex

                            state.copy(exchanges = details.exchanges, selectedExchangeIndex = selectedIndex)
                        }
                    }
                }
            }
        }

        /**
         * Разница между биржами в блоке сверху — лучшая пара бэкенда, та же, что в
         * каталоге и на экране сравнения. Раньше блок считал её сам по разбивке,
         * которая приходит без фильтра свежести, и показывал арбитраж там, где
         * одна из бирж просто зависла. Ошибку не показываем: блок останется без
         * лучшей пары и скажет об этом сам, а цены бирж ниже на месте.
         */
        private fun loadBestPrices(ticker: String) {
            if (ticker.isBlank()) return

            viewModelScope.launch {
                getBestPricesUseCase(ticker, symbolId).onSuccess { bestPrices ->
                    _uiState.update { it.copy(bestPrices = bestPrices) }
                }
            }
        }

        /**
         * Живой последний бар графика. Соединение может быть закрыто после ухода
         * с каталога, поэтому сначала connect(): он идемпотентен. Подписка одна на
         * весь экран — тики фильтруются по тикеру пары.
         */
        private fun observeLivePrice(ticker: String) {
            if (ticker.isBlank()) return

            streamConnectUseCase()
            takeOverTickerSubscriptionsUseCase(setOf(ticker))
            subscriptionTakenOver = true

            viewModelScope.launch {
                try {
                    observeTickerEventUseCase().collect { event ->
                        when {
                            event is TickerStreamEvent.TickerPriceChange &&
                                event.data.ticker == ticker &&
                                isOwnSymbol(event.data.symbolId.toLong()) -> {
                                synchronized(pendingTickLock) {
                                    pendingTicks[event.data.providerId] = event.data
                                }
                                scheduleTickFlush()
                            }

                            // тип 5 двигает блок разницы: тот же источник, что при загрузке
                            event is TickerStreamEvent.TickerBestPriceChange &&
                                event.data.ticker == ticker &&
                                isOwnSymbol(event.data.symbolId) -> {
                                synchronized(pendingTickLock) {
                                    pendingBestPrices[event.data.symbolId] = event.data
                                }
                                scheduleTickFlush()
                            }
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

                    val (ticks, bestPrices) =
                        synchronized(pendingTickLock) {
                            if (pendingTicks.isEmpty() && pendingBestPrices.isEmpty()) {
                                isTickFlushScheduled = false
                                return@launch
                            }
                            val batch = pendingTicks.values.toList() to pendingBestPrices.values.toList()
                            pendingTicks.clear()
                            pendingBestPrices.clear()
                            batch
                        }
                    ticks.forEach(::applyLiveTick)
                    if (bestPrices.isNotEmpty()) {
                        _uiState.update { it.copy(bestPrices = it.bestPrices.withUpdates(bestPrices)) }
                    }
                }
            }
        }

        /** Один тик двигает цены карточек бирж, а последний бар — только у своей биржи. */
        private fun applyLiveTick(tick: TickerPrice) {
            val state = _uiState.value
            // цены карточек двигаем всегда: у каждой биржи свой тик
            val exchanges = state.exchanges.withLivePrices(tick, System.currentTimeMillis())

            // последний бар графика двигает только тик выбранной биржи — по ней он и построен
            val selectedProviderId = state.selectedExchange?.provider?.id
            val key = selectedProviderId?.let { ChartKey(it, state.timeframe) }
            val history = key?.let { chartsByKey[it] }
            if (key != null && history != null && tick.providerId == selectedProviderId) {
                // цена бара — середина спреда: priceSell это ask, priceBuy это bid
                val midPrice = (tick.priceSell + tick.priceBuy) / 2.0
                val updated =
                    history.withLiveCandles(
                        updateLastCandle(
                            candles = history.candles,
                            price = midPrice,
                            timeframe = state.timeframe,
                            nowMillis = System.currentTimeMillis(),
                        ),
                    )
                chartsByKey.putRecent(key, updated, PairsConstants.Chart.MAX_CACHED_CHARTS)
                _uiState.update {
                    it.copy(candles = updated.candles, liveCount = updated.liveCount, exchanges = exchanges)
                }
            } else {
                _uiState.update { it.copy(exchanges = exchanges) }
            }
        }

        private fun loadPairDetails(ticker: String) {
            _uiState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                try {
                    // настройки читаются до деталей: с чего открыть пару, решают они,
                    // а сбой чтения не должен мешать экрану открыться
                    val preferences = runCatching { getMarketPreferencesUseCase() }.getOrDefault(MarketPreferences())

                    val result = getPairDetailsUseCase(ticker, symbolId)
                    result.fold(
                        onSuccess = { details ->
                            _uiState.update { state ->
                                state.copy(
                                    loading = false,
                                    networks = details.networks,
                                    exchanges = details.exchanges,
                                    selectedExchangeIndex = details.exchanges.preferredIndex(preferences),
                                    timeframe = preferences.timeframe,
                                    indicators = preferences.indicators,
                                )
                            }
                            // график строится по выбранной бирже
                            _uiState.value.selectedExchange?.provider?.id?.let { providerId ->
                                loadCandles(providerId, ticker, _uiState.value.timeframe)
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(loading = false, error = error.asAppError()) }
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(loading = false, error = e.asAppError()) }
                }
            }
        }

        /**
         * Биржа, с которой открывается пара: из настроек, если она у этой пары есть,
         * иначе первая — у половины пар выбранной площадки просто нет.
         */
        private fun List<ProviderDetail>.preferredIndex(preferences: MarketPreferences): Int =
            indexOfFirst { it.provider.id == preferences.defaultProviderId }.takeIf { it >= 0 } ?: 0

        /** Средняя включается и выключается на самом графике: набор общий и запоминается. */
        fun onIndicatorToggled(indicator: ChartIndicator) {
            val updated =
                _uiState.value.indicators
                    .toMutableSet()
                    .apply { if (!add(indicator)) remove(indicator) }
                    .toSet()

            _uiState.update { it.copy(indicators = updated) }
            viewModelScope.launch { setChartIndicatorsUseCase(updated) }
        }

        fun onTimeframeSelected(timeframe: ChartTimeframe) {
            if (_uiState.value.timeframe == timeframe) return
            _uiState.update { it.copy(timeframe = timeframe) }
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return
            loadCandles(providerId, _uiState.value.ticker, timeframe)
        }

        fun onExchangeSelected(index: Int) {
            if (_uiState.value.selectedExchangeIndex == index) return
            _uiState.update { it.copy(selectedExchangeIndex = index) }
            // график привязан к бирже: при смене перегружаем (из кеша, если уже был)
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return
            loadCandles(providerId, _uiState.value.ticker, _uiState.value.timeframe)
        }

        /**
         * Первая страница истории для (биржа, масштаб). Глубина набирается только
         * страницами: у части бирж бэкенд отдаёт максимум [PairsConstants.Chart.PAGE_LIMIT]
         * свечей за запрос. Повторный заход на тот же ключ берёт уже загруженное из памяти.
         */
        private fun loadCandles(
            providerId: Int,
            symbol: String,
            timeframe: ChartTimeframe,
        ) {
            val key = ChartKey(providerId, timeframe)
            chartsByKey[key]?.let { cached ->
                // вернулись к графику — он снова свежий, вытеснять надо другие
                chartsByKey.putRecent(key, cached, PairsConstants.Chart.MAX_CACHED_CHARTS)
                emitChart(cached)
                return
            }

            _uiState.update {
                it.copy(
                    candles = emptyList(),
                    liveCount = 0,
                    chartLoading = true,
                    chartLoadingOlder = false,
                    chartCanLoadOlder = false,
                )
            }
            viewModelScope.launch {
                getTickerHistoryUseCase(
                    providerId,
                    symbol,
                    timeframe,
                    PairsConstants.Chart.PAGE_LIMIT,
                    offset = 0,
                ).onSuccess { page ->
                    val history = ChartHistory.initial(page)
                    chartsByKey.putRecent(key, history, PairsConstants.Chart.MAX_CACHED_CHARTS)
                    if (isCurrentChart(providerId, timeframe)) emitChart(history)
                }.onFailure { error ->
                    // сбой графика не ломает экран: цены и биржи остаются доступны
                    if (error is CancellationException) throw error
                    if (isCurrentChart(providerId, timeframe)) _uiState.update { it.copy(chartLoading = false) }
                }
            }
        }

        /**
         * Следующая страница истории — её просит сам график, когда до левого края
         * загруженного остаётся меньше экрана. Свечи дописываются слева; кадр от
         * этого не двигается, потому что считается в абсолютных индексах свечей.
         */
        fun loadOlderCandles() {
            val key = currentChartKey() ?: return
            val history = chartsByKey[key] ?: return
            val state = _uiState.value
            if (!history.canLoadOlder || state.chartLoading || state.chartLoadingOlder) return

            _uiState.update { it.copy(chartLoadingOlder = true) }
            viewModelScope.launch {
                getTickerHistoryUseCase(
                    key.providerId,
                    state.ticker,
                    key.timeframe,
                    PairsConstants.Chart.PAGE_LIMIT,
                    offset = history.oldestSkip,
                ).onSuccess { page ->
                    // пока страница ехала, живой тик мог дорисовать бар — берём свежее
                    val current = chartsByKey[key] ?: return@onSuccess
                    val updated = current.withOlderPage(page)
                    chartsByKey.putRecent(key, updated, PairsConstants.Chart.MAX_CACHED_CHARTS)
                    if (isCurrentChart(key.providerId, key.timeframe)) emitChart(updated)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    if (isCurrentChart(key.providerId, key.timeframe)) {
                        _uiState.update { it.copy(chartLoadingOlder = false) }
                    }
                }
            }
        }

        private fun currentChartKey(): ChartKey? {
            val providerId =
                _uiState.value.selectedExchange
                    ?.provider
                    ?.id ?: return null
            return ChartKey(providerId, _uiState.value.timeframe)
        }

        private fun emitChart(history: ChartHistory) {
            _uiState.update {
                it.copy(
                    candles = history.candles,
                    liveCount = history.liveCount,
                    chartLoading = false,
                    chartLoadingOlder = false,
                    chartCanLoadOlder = history.canLoadOlder,
                )
            }
        }

        // пока история ехала, пользователь мог сменить биржу или масштаб — тогда
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

        /**
         * Событие своего символа. Подписка в сокете идёт по тикеру, и события всех
         * его символов приходят вперемешку — чужие сети сюда не пускаем.
         */
        private fun isOwnSymbol(eventSymbolId: Long): Boolean = symbolId == null || eventSymbolId == symbolId

        override fun onCleared() {
            // отпускаем захват только если сами его брали — иначе чужой счётчик уедет
            // в минус и каталог вернётся раньше времени поверх активного экрана
            if (subscriptionTakenOver) {
                restoreTickerSubscriptionsUseCase()
            }
            super.onCleared()
        }
    }
