package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.portfolio.util.PortfolioConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
        private val applyBestPriceChangesUseCase: ApplyBestPriceChangesUseCase,
        private val refreshBestPricesUseCase: RefreshBestPricesUseCase,
        private val refreshPinnedQuotesUseCase: RefreshPinnedQuotesUseCase,
        private val applyPinnedPriceTicksUseCase: ApplyPinnedPriceTicksUseCase,
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

        private var portfolioJob: Job? = null
        private var liveJob: Job? = null
        private var subscriptionsTakenOver = false

        /**
         * Лучшие пары из сокета за интервал. Ключ — `symbolId`: у тикера их
         * бывает несколько, по одному на сеть, и одна переменная на всех хранила
         * бы только того, кто тикнул последним.
         *
         * Без блокировки: и сбор, и сброс идут в `viewModelScope`, то есть на
         * одном потоке.
         */
        private val pendingBestPrices = mutableMapOf<Long, TickerBestPrice>()

        /**
         * Котировки бирж, за которыми закреплены позиции, по `symbolId`. Событие
         * типа 4 несёт котировку одной биржи, и из всех бирж тикера нужна ровно
         * одна на позицию — остальные отбрасываются ещё до пачки.
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
            pendingBestPrices.clear()
            pendingPinnedTicks.clear()
            subscribedTickers.clear()
            refreshedSources.clear()
            refreshingSources.clear()
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
         */
        private fun refreshPrices(force: Boolean) {
            if (force) {
                refreshedSources.clear()
                refreshingSources.clear()
            }

            val pending =
                positions.filter { position ->
                    val source = position.priceSource()
                    source !in refreshedSources && source !in refreshingSources
                }
            if (pending.isEmpty()) return

            val (pinned, best) = pending.partition { it.providerId != null }
            val bestSources = best.map { it.priceSource() }.toSet()
            val pinnedSources = pinned.map { it.priceSource() }.toSet()

            refreshingSources += bestSources + pinnedSources
            viewModelScope.launch {
                // Ноль обновлённых котировок — это «не прошёл ни один запрос», а не
                // «цены свежие»: use case'ы глотают ошибки по отдельным тикерам.
                // Пометить такие источники догнанными значило бы не вернуться к ним
                // никогда — в офлайне так и было бы: экран открыт, цены старые, повторов нет.
                if (bestSources.isNotEmpty()) {
                    val tickers = bestSources.filterIsInstance<PriceSource.Best>().map { it.ticker }.toSet()
                    val updated = refreshBestPricesUseCase(tickers).getOrDefault(0)

                    refreshingSources -= bestSources
                    if (updated > 0) refreshedSources += bestSources
                }

                if (pinnedSources.isNotEmpty()) {
                    val updated = refreshPinnedQuotesUseCase(pinned).getOrDefault(0)

                    refreshingSources -= pinnedSources
                    if (updated > 0) refreshedSources += pinnedSources
                }
            }
        }

        private fun observeLivePrices() {
            liveJob =
                viewModelScope.launch {
                    launch {
                        observeTickerEventUseCase().collect { event ->
                            when {
                                event is TickerStreamEvent.TickerBestPriceChange -> {
                                    pendingBestPrices[event.data.symbolId] = event.data
                                    scheduleFlush()
                                }

                                event is TickerStreamEvent.TickerPriceChange && event.data.isPinnedQuote() -> {
                                    pendingPinnedTicks[event.data.symbolId.toLong()] = event.data
                                    scheduleFlush()
                                }
                            }
                        }
                    }

                    launch {
                        observeStreamReconnectsUseCase().collect { refreshPrices(force = true) }
                    }
                }
        }

        /** Котировка биржи, за которой закреплена позиция этого символа. */
        private fun TickerPrice.isPinnedQuote(): Boolean =
            positions.any { it.symbolId == symbolId.toLong() && it.providerId == providerId }

        /**
         * Тики уходят в базу пачками: строку каталога и последние цены бирж
         * позиций пишет Room, а он сам разошлёт их и списку, и портфелю. Джоб
         * живёт, пока тики идут: один пустой интервал — и он выходит.
         */
        private fun scheduleFlush() {
            if (isFlushScheduled) return
            isFlushScheduled = true

            viewModelScope.launch {
                while (true) {
                    delay(PortfolioConstants.Prices.FLUSH_INTERVAL_MS.milliseconds)

                    if (pendingBestPrices.isEmpty() && pendingPinnedTicks.isEmpty()) {
                        isFlushScheduled = false
                        return@launch
                    }

                    val bestPrices = pendingBestPrices.values.toList()
                    val pinnedTicks = pendingPinnedTicks.values.toList()
                    pendingBestPrices.clear()
                    pendingPinnedTicks.clear()
                    if (bestPrices.isNotEmpty()) applyBestPriceChangesUseCase(bestPrices)
                    if (pinnedTicks.isNotEmpty()) applyPinnedPriceTicksUseCase(pinnedTicks)
                }
            }
        }

        override fun onCleared() {
            onScreenHidden()
            super.onCleared()
        }
    }
