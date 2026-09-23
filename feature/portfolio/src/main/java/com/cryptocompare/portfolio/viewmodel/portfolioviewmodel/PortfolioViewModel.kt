package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.pairs.GetCatalogLastUpdateUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveConnectionStateUseCase
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
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.ticker.TickerConnectionState
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Портфель: позиции из базы, цены — из каталога, а у позиций с указанной
 * биржей — последние котировки этой биржи.
 *
 * Пока экран открыт, он забирает подписки соединения себе, как это делают
 * детали и сравнение: слотов мало, а каталога под портфелем не видно. Отдаёт
 * их обратно при уходе с экрана, а не в `onCleared` — вкладка сохраняет свою
 * ViewModel живой, и каталог ждал бы свои подписки до самого закрытия
 * приложения.
 */
@HiltViewModel
class PortfolioViewModel
    @Inject
    constructor(
        private val observePortfolioUseCase: ObservePortfolioUseCase,
        private val observePortfolioPricesUseCase: ObservePortfolioPricesUseCase,
        private val calculatePortfolioUseCase: CalculatePortfolioUseCase,
        private val streamConnectUseCase: StreamConnectUseCase,
        private val takeOverTickerSubscriptionsUseCase: TakeOverTickerSubscriptionsUseCase,
        private val restoreTickerSubscriptionsUseCase: RestoreTickerSubscriptionsUseCase,
        private val syncVisibleTickersUseCase: SyncVisibleTickersUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
        private val observeStreamReconnectsUseCase: ObserveStreamReconnectsUseCase,
        private val refreshBestPricesUseCase: RefreshBestPricesUseCase,
        private val refreshPinnedQuotesUseCase: RefreshPinnedQuotesUseCase,
        private val applyPinnedPriceTicksUseCase: ApplyPinnedPriceTicksUseCase,
        private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
        private val getCatalogLastUpdateUseCase: GetCatalogLastUpdateUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PortfolioUiState())
        val uiState = _uiState.asStateFlow()

        /** Позиции в порядке списка: подписки достаются верхним. */
        private var positions: List<PortfolioPosition> = emptyList()
        private val subscribedTickers = mutableSetOf<String>()

        /** Чьи цены уже дотянули по REST в этот заход — чтобы не ходить за той же ценой дважды. */
        private val refreshedSources = mutableSetOf<PriceSource>()

        /** Кто в пути прямо сейчас: без этого повторная правка позиций слала бы те же запросы заново. */
        private val refreshingSources = mutableSetOf<PriceSource>()

        /** Поток был живым в этот показ экрана: его обрыв — и есть время, на котором цены замерли. */
        private var wasLive = false

        private var portfolioJob: Job? = null
        private var liveJob: Job? = null
        private var subscriptionsTakenOver = false

        /**
         * Котировки бирж, за которыми закреплены позиции, по `symbolId`. Событие
         * типа 4 несёт котировку одной биржи, и из всех бирж тикера нужна ровно
         * одна на позицию — остальные отбрасываются ещё до пачки.
         *
         * Лучшие пары (тип 5) портфель не пишет: их пишет в каталог
         * `SyncLiveBestPricesUseCase`, один на приложение, а портфель читает
         * каталог. Без блокировки: и сбор, и сброс идут в `viewModelScope`, то
         * есть на одном потоке.
         */
        private val pendingPinnedTicks = mutableMapOf<Long, TickerPrice>()
        private var isFlushScheduled = false

        /**
         * Экран показался: считаем портфель, берём соединение, захватываем
         * подписки и догоняем цены по REST.
         *
         * Всё это начинается здесь, а не в `init`: ViewModel вкладки переживает
         * уход с неё, и запущенный в `init` сбор пересчитывал бы портфель на
         * каждый сброс цен каталога — при закрытом портфеле и никому не нужный.
         *
         * Догонка нужна ровно потому, что портфель живёт отдельно от каталога:
         * его пары могли ни разу не попасть на экран, и цена у них — со времени
         * последней синхронизации каталога. Сокет прошлое не досылает.
         */
        fun onScreenShown() {
            if (subscriptionsTakenOver) return

            observePortfolio()

            streamConnectUseCase()
            // забираем слоты себе пустым набором, а кем их занять — решает
            // syncSubscriptions: лимит подписок живёт там, в одном месте
            takeOverTickerSubscriptionsUseCase(emptySet())
            subscriptionsTakenOver = true
            subscribedTickers.clear()

            syncSubscriptions()
            refreshPrices(force = true)
            observeLivePrices()
            loadLastUpdate()
        }

        /**
         * «Обновить» на полоске замерших цен: сокет ещё переподключается с
         * бэкоффом, а цены позиций можно взять по REST прямо сейчас.
         */
        fun onRefreshClick() {
            refreshPrices(force = true) { updated ->
                // ноль — это «ни один запрос не прошёл»: молча оставить время
                // нетронутым значило бы сделать вид, что кнопка сработала
                if (updated == 0 && positions.isNotEmpty()) {
                    _uiState.update { it.copy(refreshFailed = true) }
                }
            }
        }

        fun onRefreshFailureShown() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /**
         * Экран ушёл: возвращаем подписки каталогу, перестаём слушать тики и
         * считать портфель. Состояние остаётся на месте — при следующем показе
         * Room отдаёт позиции сразу, и список не мигает загрузкой.
         */
        fun onScreenHidden() {
            if (!subscriptionsTakenOver) return

            portfolioJob?.cancel()
            portfolioJob = null
            liveJob?.cancel()
            liveJob = null
            pendingPinnedTicks.clear()
            subscribedTickers.clear()
            refreshedSources.clear()
            refreshingSources.clear()
            wasLive = false
            subscriptionsTakenOver = false
            restoreTickerSubscriptionsUseCase()
        }

        /**
         * Позиции лежат в базе, цены — в каталоге и в последних котировках бирж
         * позиций, и портфель это их произведение.
         *
         * Набор символов меняется вместе с позициями, поэтому подписка на цены
         * пересоздаётся: `flatMapLatest` снимает прошлую, и удалённая позиция не
         * тянет за собой наблюдение за своей ценой.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observePortfolio() {
            portfolioJob =
                viewModelScope.launch {
                    observePortfolioUseCase()
                        .flatMapLatest { positions ->
                            observePortfolioPricesUseCase(positions)
                                .map { quotes -> positions to calculatePortfolioUseCase(positions, quotes) }
                        }.collect { (positions, portfolio) ->
                            this@PortfolioViewModel.positions = positions

                            _uiState.update {
                                it.copy(
                                    holdings = portfolio.holdings,
                                    summary = portfolio.summary,
                                    loading = false,
                                )
                            }

                            // позиция могла появиться, пока экран открыт: её пара в
                            // подписках не числится, а цену ей взять неоткуда
                            if (subscriptionsTakenOver) {
                                syncSubscriptions()
                                refreshPrices(force = false)
                            }
                        }
                }
        }

        private fun syncSubscriptions() {
            val updated = syncVisibleTickersUseCase(positions.map { it.ticker }, subscribedTickers)

            subscribedTickers.clear()
            subscribedTickers.addAll(updated)
        }

        /**
         * Цены позиций через REST. [force] — взять всё заново: так при открытии
         * экрана и после реконнекта, когда пропущенное сервер не досылает.
         * Иначе догоняются только те, за кем ещё не ходили — в том числе позиция,
         * у которой только что сменили биржу: источник цены у неё теперь другой.
         *
         * [onDone] получает число обновлённых цен — по нему «Обновить» решает,
         * сработала ли кнопка.
         */
        private fun refreshPrices(
            force: Boolean,
            onDone: ((updated: Int) -> Unit)? = null,
        ) {
            if (force) {
                refreshedSources.clear()
                refreshingSources.clear()
            }

            val pending =
                positions.filter { position ->
                    val source = position.priceSource()
                    source !in refreshedSources && source !in refreshingSources
                }
            if (pending.isEmpty()) {
                onDone?.invoke(0)
                return
            }

            val (pinned, best) = pending.partition { it.providerId != null }
            val bestSources = best.map { it.priceSource() }.toSet()
            val pinnedSources = pinned.map { it.priceSource() }.toSet()

            refreshingSources += bestSources + pinnedSources
            viewModelScope.launch {
                // Ноль обновлённых котировок — это «не прошёл ни один запрос», а не
                // «цены свежие»: use case'ы глотают ошибки по отдельным тикерам.
                // Пометить такие источники догнанными значило бы не вернуться к ним
                // никогда — в офлайне так и было бы: экран открыт, цены старые, повторов нет.
                var total = 0

                if (bestSources.isNotEmpty()) {
                    val tickers = bestSources.filterIsInstance<PriceSource.Best>().map { it.ticker }.toSet()
                    val updated = refreshBestPricesUseCase(tickers).getOrDefault(0)

                    refreshingSources -= bestSources
                    if (updated > 0) refreshedSources += bestSources
                    total += updated
                }

                if (pinnedSources.isNotEmpty()) {
                    val updated = refreshPinnedQuotesUseCase(pinned).getOrDefault(0)

                    refreshingSources -= pinnedSources
                    if (updated > 0) refreshedSources += pinnedSources
                    total += updated
                }

                if (total > 0) markUpdated()
                onDone?.invoke(total)
            }
        }

        private fun observeLivePrices() {
            liveJob =
                viewModelScope.launch {
                    launch {
                        observeTickerEventUseCase().collect { event ->
                            if (event is TickerStreamEvent.TickerPriceChange && event.data.isPinnedQuote()) {
                                pendingPinnedTicks[event.data.symbolId.toLong()] = event.data
                                scheduleFlush()
                            }
                        }
                    }

                    launch {
                        observeStreamReconnectsUseCase().collect { refreshPrices(force = true) }
                    }

                    launch { observeStaleStream() }
                }
        }

        /**
         * Полоска «цены не обновляются» — как в каталоге: не на первой же секунде
         * без связи, а когда поток пролежал [WebSocketConstants.STALE_NOTICE_DELAY_MS].
         *
         * Время, на котором цены замерли, — момент обрыва: пока поток жив, цены
         * на экране текущие, и вести время по каждому тику незачем (это
         * обновляло бы экран на каждый тик).
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private suspend fun observeStaleStream() {
            observeConnectionStateUseCase()
                .map { state -> state is TickerConnectionState.Connected }
                .distinctUntilChanged()
                .onEach { live ->
                    if (wasLive && !live) markUpdated()
                    wasLive = live
                }.flatMapLatest { live ->
                    if (live) {
                        flowOf(false)
                    } else {
                        flow {
                            delay(WebSocketConstants.STALE_NOTICE_DELAY_MS.milliseconds)
                            emit(true)
                        }
                    }
                }.distinctUntilChanged()
                .collect { stale -> _uiState.update { it.copy(isStale = stale) } }
        }

        private fun markUpdated() {
            _uiState.update { it.copy(lastUpdateMillis = System.currentTimeMillis()) }
        }

        /** Отправная точка для «цены не обновляются · 13:48», когда портфель открыли без сети. */
        private fun loadLastUpdate() {
            viewModelScope.launch {
                val lastUpdate =
                    try {
                        getCatalogLastUpdateUseCase()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        // без отправной точки полоска просто скажет «цены не обновляются»
                        null
                    } ?: return@launch

                _uiState.update { state ->
                    if (state.lastUpdateMillis == null) state.copy(lastUpdateMillis = lastUpdate) else state
                }
            }
        }

        /** Котировка биржи, за которой закреплена позиция этого символа. */
        private fun TickerPrice.isPinnedQuote(): Boolean =
            positions.any { it.symbolId == symbolId.toLong() && it.providerId == providerId }

        /**
         * Цены бирж покупки уходят в базу пачками, а Room сам разошлёт их
         * портфелю. Джоб живёт, пока тики идут: один пустой интервал — и он выходит.
         */
        private fun scheduleFlush() {
            if (isFlushScheduled) return
            isFlushScheduled = true

            viewModelScope.launch {
                while (true) {
                    delay(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS.milliseconds)

                    if (pendingPinnedTicks.isEmpty()) {
                        isFlushScheduled = false
                        return@launch
                    }

                    val pinnedTicks = pendingPinnedTicks.values.toList()
                    pendingPinnedTicks.clear()
                    applyPinnedPriceTicksUseCase(pinnedTicks)
                }
            }
        }

        override fun onCleared() {
            onScreenHidden()
            super.onCleared()
        }
    }
