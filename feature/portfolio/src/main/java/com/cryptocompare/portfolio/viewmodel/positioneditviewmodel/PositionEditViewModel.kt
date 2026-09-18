package com.cryptocompare.portfolio.viewmodel.positioneditviewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.portfolio.DeletePortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.GetPortfolioPositionUseCase
import com.cryptocompare.domain.usecase.portfolio.SavePortfolioPositionUseCase
import com.cryptocompare.model.portfolio.PortfolioPositionDraft
import com.cryptocompare.portfolio.util.PortfolioConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class PositionEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getPortfolioPositionUseCase: GetPortfolioPositionUseCase,
        private val savePortfolioPositionUseCase: SavePortfolioPositionUseCase,
        private val deletePortfolioPositionUseCase: DeletePortfolioPositionUseCase,
    ) : ViewModel() {
        private val symbolId: Long = savedStateHandle.get<Long>(PortfolioConstants.Navigation.SYMBOL_ID_ARG) ?: 0L
        private val ticker: String = savedStateHandle.get<String>(PortfolioConstants.Navigation.TICKER_ARG).orEmpty()

        /** Текущий ask выбранной биржи: обычно покупают по рынку, и цену не надо набирать руками. */
        private val suggestedPrice: Double? =
            savedStateHandle.get<String>(PortfolioConstants.Navigation.PRICE_ARG)?.toDoubleOrNull()

        private val _uiState =
            MutableStateFlow(
                PositionEditUiState(
                    symbolId = symbolId,
                    ticker = ticker,
                    priceInput = suggestedPrice?.toInput().orEmpty(),
                ),
            )
        val uiState = _uiState.asStateFlow()

        init {
            loadExisting()
        }

        fun onAmountChange(input: String) {
            _uiState.update { it.copy(amountInput = input, saveFailed = false) }
        }

        fun onPriceChange(input: String) {
            _uiState.update { it.copy(priceInput = input, saveFailed = false) }
        }

        fun onSave() {
            val state = _uiState.value
            if (!state.canSave) return
            val amount = state.amount ?: return
            val price = state.price ?: return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, saveFailed = false) }
                savePortfolioPositionUseCase(PortfolioPositionDraft(symbolId, ticker, amount, price))
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isDone = true) } }
                    .onFailure { _uiState.update { it.copy(isSaving = false, saveFailed = true) } }
            }
        }

        fun onDelete() {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, saveFailed = false) }
                deletePortfolioPositionUseCase(symbolId)
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isDone = true) } }
                    .onFailure { _uiState.update { it.copy(isSaving = false, saveFailed = true) } }
            }
        }

        /** Позиция на символ одна: если она уже есть, форма открывается с её числами. */
        private fun loadExisting() {
            viewModelScope.launch {
                val existing = runCatching { getPortfolioPositionUseCase(symbolId) }.getOrNull() ?: return@launch

                _uiState.update { state ->
                    state.copy(
                        amountInput = existing.amount.toInput(),
                        priceInput = existing.buyPrice.toInput(),
                        isExisting = true,
                    )
                }
            }
        }

        /** Без экспоненты и хвостовых нулей: «72000», а не «7.2E4» и не «72000.0». */
        private fun Double.toInput(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
    }
