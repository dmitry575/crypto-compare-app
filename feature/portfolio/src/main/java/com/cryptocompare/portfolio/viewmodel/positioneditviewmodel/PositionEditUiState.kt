package com.cryptocompare.portfolio.viewmodel.positioneditviewmodel

import com.cryptocompare.portfolio.util.parseDecimalInput

data class PositionEditUiState(
    val symbolId: Long,
    val ticker: String,
    val amountInput: String = "",
    val priceInput: String = "",
    /** Позиция по символу уже есть: форма правит её, и появляется «Удалить». */
    val isExisting: Boolean = false,
    val isSaving: Boolean = false,
    /** Сохранили или удалили — экран закрывается. */
    val isDone: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val amount: Double? get() = amountInput.parseDecimalInput()
    val price: Double? get() = priceInput.parseDecimalInput()

    /**
     * Ноль количества сохранять нечего: удаление — отдельная кнопка, а не
     * «сохранить пустое». Цена ноль допустима — так выглядят аирдропы.
     */
    val canSave: Boolean get() = !isSaving && (amount ?: 0.0) > 0.0 && price != null
}
