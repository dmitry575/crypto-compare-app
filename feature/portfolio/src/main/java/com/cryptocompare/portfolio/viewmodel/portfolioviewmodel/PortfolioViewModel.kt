package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.portfolio.ObservePortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel
    @Inject
    constructor(
        observePortfolioUseCase: ObservePortfolioUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PortfolioUiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observePortfolioUseCase().collect { positions ->
                    _uiState.update { it.copy(positions = positions, loading = false) }
                }
            }
        }
    }
