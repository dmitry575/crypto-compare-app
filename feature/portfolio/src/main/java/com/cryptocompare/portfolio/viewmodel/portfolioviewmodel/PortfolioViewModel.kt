package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.portfolio.CalculatePortfolioUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioPricesUseCase
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import com.cryptocompare.model.portfolio.PortfolioPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel
    @Inject
    constructor(
        private val observePortfolioUseCase: ObservePortfolioUseCase,
        private val observePortfolioPricesUseCase: ObservePortfolioPricesUseCase,
        private val calculatePortfolioUseCase: CalculatePortfolioUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PortfolioUiState())
        val uiState = _uiState.asStateFlow()

        init {
            observePortfolio()
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
            viewModelScope.launch {
                observePortfolioUseCase()
                    .flatMapLatest { positions ->
                        observePortfolioPricesUseCase(positions.map(PortfolioPosition::symbolId).toSet())
                            .map { prices -> calculatePortfolioUseCase(positions, prices) }
                    }.collect { portfolio ->
                        _uiState.update {
                            it.copy(
                                holdings = portfolio.holdings,
                                summary = portfolio.summary,
                                loading = false,
                            )
                        }
                    }
            }
        }
    }
