package com.cryptocompare.pairs.viewmodel.comparisonViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.pairs.ComparePairAcrossExchangesUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RestoreTickerSubscriptionsUseCase
import com.cryptocompare.domain.usecase.pairs.StreamConnectUseCase
import com.cryptocompare.domain.usecase.pairs.SubscribeSingleTickerUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.withLivePrices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Экран сравнения котировок пары по биржам.
 *
 * Слушает **оба** события сокета, и у каждого своя работа. Тип 4 — котировка
 * одной биржи, он двигает строки таблицы по `providerId`. Тип 5 — лучшая пара,
 * отфильтрованная бэкендом от протухших котировок, он двигает выжимку сверху и
 * отметки «здесь выгоднее». Считать лучшее самим по таблице нельзя: она приходит
 * без фильтра, и биржа с зависшей ценой выигрывала бы сравнение.
 */
@HiltViewModel
class ComparisonViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val comparePairAcrossExchangesUseCase: ComparePairAcrossExchangesUseCase,
        private val streamConnectUseCase: StreamConnectUseCase,
        private val subscribeSingleTickerUseCase: SubscribeSingleTickerUseCase,
        private val restoreTickerSubscriptionsUseCase: RestoreTickerSubscriptionsUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ComparisonUiState())
        val uiState = _uiState.asStateFlow()

        /** Захватили ли подписку под этот экран — чтобы отпустить её ровно один раз. */
        private var subscriptionTakenOver = false

        private val pendingLock = Any()

        @Volatile private var isFlushScheduled = false

        /** Последний тик каждой биржи и последняя лучшая пара, накопленные за интервал. */
        private val pendingQuotes = mutableMapOf<Int, TickerPrice>()
        private var pendingBest: TickerBestPrice? = null

        init {
            val ticker = savedStateHandle.get<String>(PairsConstants.Navigation.TICKER_ARG)?.lowercase() ?: ""
            _uiState.update { it.copy(ticker = ticker) }
            loadComparison(ticker)
            observeLivePrices(ticker)
        }

        fun retry() {
            _uiState.update { it.copy(loading = true, error = null) }
            loadComparison(_uiState.value.ticker)
        }

        fun onErrorShown() {
            _uiState.update { it.copy(error = null) }
        }

        private fun loadComparison(ticker: String) {
            if (ticker.isBlank()) {
                _uiState.update { it.copy(loading = false) }
                return
            }

            viewModelScope.launch {
                comparePairAcrossExchangesUseCase(ticker).fold(
                    onSuccess = { comparison ->
                        _uiState.update { it.copy(loading = false, comparison = comparison) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(loading = false, error = error.toUserMessage()) }
                    },
                )
            }
        }

        private fun observeLivePrices(ticker: String) {
            if (ticker.isBlank()) return

            // connect идемпотентен: соединение могло быть закрыто после ухода с каталога
            streamConnectUseCase()
            subscribeSingleTickerUseCase(ticker)
            subscriptionTakenOver = true

            viewModelScope.launch {
                try {
                    observeTickerEventUseCase().collect { event ->
                        when {
                            event is TickerStreamEvent.TickerPriceChange && event.data.ticker == ticker -> {
                                synchronized(pendingLock) { pendingQuotes[event.data.providerId] = event.data }
                                scheduleFlush()
                            }

                            event is TickerStreamEvent.TickerBestPriceChange && event.data.ticker == ticker -> {
                                synchronized(pendingLock) { pendingBest = event.data }
                                scheduleFlush()
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.toUserMessage()) }
                }
            }
        }

        /**
         * Тики приходят десятками в секунду, состояние обновляется раз в интервал
         * накопленным. Джоб живёт, пока тики идут: один пустой интервал — и он выходит.
         */
        private fun scheduleFlush() {
            synchronized(pendingLock) {
                if (isFlushScheduled) return
                isFlushScheduled = true
            }

            viewModelScope.launch {
                while (true) {
                    delay(PairsConstants.ComparisonScreen.LIVE_PRICE_INTERVAL_MS.milliseconds)

                    val (quotes, best) =
                        synchronized(pendingLock) {
                            if (pendingQuotes.isEmpty() && pendingBest == null) {
                                isFlushScheduled = false
                                return@launch
                            }
                            val batch = pendingQuotes.values.toList() to pendingBest
                            pendingQuotes.clear()
                            pendingBest = null
                            batch
                        }

                    applyLiveUpdates(quotes, best)
                }
            }
        }

        private fun applyLiveUpdates(
            quotes: List<TickerPrice>,
            best: TickerBestPrice?,
        ) {
            _uiState.update { state ->
                val comparison = state.comparison ?: return@update state

                val updatedQuotes =
                    quotes.fold(comparison.quotes) { acc, tick -> acc.withLivePrices(tick) }

                state.copy(
                    comparison =
                        comparison.copy(
                            quotes = updatedQuotes,
                            bestAskProviderId = best?.bestAskProviderId ?: comparison.bestAskProviderId,
                            bestAskPrice = best?.bestAskPrice?.takeIf { it > 0 } ?: comparison.bestAskPrice,
                            bestBidProviderId = best?.bestBidProviderId ?: comparison.bestBidProviderId,
                            bestBidPrice = best?.bestBidPrice?.takeIf { it > 0 } ?: comparison.bestBidPrice,
                            spreadPercent = best?.spreadPercent ?: comparison.spreadPercent,
                        ),
                )
            }
        }

        override fun onCleared() {
            // отпускаем захват только если сами его брали — иначе чужой счётчик уедет
            // в минус и каталог вернётся раньше времени поверх активного экрана
            if (subscriptionTakenOver) {
                restoreTickerSubscriptionsUseCase()
            }
            super.onCleared()
        }
    }
