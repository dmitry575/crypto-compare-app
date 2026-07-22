package com.cryptocompare.pairs.viewmodel.detailViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.pairs.GetTickerDetailUseCase
import com.cryptocompare.domain.usecase.pairs.GetTickerHistoryUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.util.PairsConstants
import dagger.hilt.android.lifecycle.HiltViewModel
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DetailUiState())
        val uiState = _uiState.asStateFlow()

        /** Загруженные масштабы: повторное переключение не идёт в сеть. */
        private val candlesByTimeframe = mutableMapOf<ChartTimeframe, List<Candle>>()

        init {
            val ticker = savedStateHandle.get<String>(PairsConstants.Navigation.TICKER_ARG)?.lowercase() ?: ""
            _uiState.update { it.copy(ticker = ticker) }
            loadPairDetails(ticker)
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
    }
