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
import com.cryptocompare.domain.usecase.portfolio.CalculatePortfolioUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioPricesUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.ticker.TickerBestPrice
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
 * Портфель: позиции из базы, цены — из каталога.
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PortfolioUiState())
        val uiState = _uiState.asStateFlow()

        /** Тикеры позиций в порядке списка: подписки достаются верхним. */
        private var positionTickers: List<String> = emptyList()
        private val subscribedTickers = mutableSetOf<String>()

        /** Кого уже дотянули по REST в этот заход — чтобы не ходить за той же ценой дважды. */
        private val refreshedTickers = mutableSetOf<String>()

        /** Кто в пути прямо сейчас: без этого повторная правка позиций слала бы те же запросы заново. */
        private val refreshingTickers = mutableSetOf<String>()

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
            subscribedTickers.clear()
            refreshedTickers.clear()
            refreshingTickers.clear()
            subscriptionsTakenOver = false
            restoreTickerSubscriptionsUseCase()
        }

        /**
         * Позиции лежат в базе, цены — в каталоге, и портфель это их произведение.
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
                            observePortfolioPricesUseCase(positions.map(PortfolioPosition::symbolId).toSet())
                                .map { prices -> positions to calculatePortfolioUseCase(positions, prices) }
                        }.collect { (positions, portfolio) ->
                            positionTickers = positions.map { it.ticker }

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
            val updated = syncVisibleTickersUseCase(positionTickers, subscribedTickers)

            subscribedTickers.clear()
            subscribedTickers.addAll(updated)
        }

        /**
         * Цены позиций через REST. [force] — взять всё заново: так при открытии
         * экрана и после реконнекта, когда пропущенное сервер не досылает.
         * Иначе догоняются только те, за кем ещё не ходили.
         */
        private fun refreshPrices(force: Boolean) {
            if (force) {
                refreshedTickers.clear()
                refreshingTickers.clear()
            }

            val tickers = positionTickers.map { it.lowercase() }.toSet() - refreshedTickers - refreshingTickers
            if (tickers.isEmpty()) return

            refreshingTickers += tickers
            viewModelScope.launch {
                // Ноль обновлённых котировок — это «не прошёл ни один запрос», а не
                // «цены свежие»: use case глотает ошибки по отдельным тикерам. Пометить
                // такие тикеры догнанными значило бы не вернуться к ним никогда — в
                // офлайне так и было бы: экран открыт, цены старые, повторов нет.
                val updated = refreshBestPricesUseCase(tickers).getOrDefault(0)

                refreshingTickers -= tickers
                if (updated > 0) refreshedTickers += tickers
            }
        }

        private fun observeLivePrices() {
            liveJob =
                viewModelScope.launch {
                    launch {
                        observeTickerEventUseCase().collect { event ->
                            if (event is TickerStreamEvent.TickerBestPriceChange) {
                                pendingBestPrices[event.data.symbolId] = event.data
                                scheduleFlush()
                            }
                        }
                    }

                    launch {
                        observeStreamReconnectsUseCase().collect { refreshPrices(force = true) }
                    }
                }
        }

        /**
         * Тики уходят в базу пачками: строку каталога пишет Room, а он сам
         * разошлёт её и списку, и портфелю. Джоб живёт, пока тики идут: один
         * пустой интервал — и он выходит.
         */
        private fun scheduleFlush() {
            if (isFlushScheduled) return
            isFlushScheduled = true

            viewModelScope.launch {
                while (true) {
                    delay(PortfolioConstants.Prices.FLUSH_INTERVAL_MS.milliseconds)

                    if (pendingBestPrices.isEmpty()) {
                        isFlushScheduled = false
                        return@launch
                    }

                    val batch = pendingBestPrices.values.toList()
                    pendingBestPrices.clear()
                    applyBestPriceChangesUseCase(batch)
                }
            }
        }

        override fun onCleared() {
            onScreenHidden()
            super.onCleared()
        }
    }
