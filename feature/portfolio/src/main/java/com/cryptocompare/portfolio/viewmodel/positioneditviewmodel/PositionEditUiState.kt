package com.cryptocompare.portfolio.viewmodel.positioneditviewmodel

import com.cryptocompare.model.provider.Provider
import com.cryptocompare.portfolio.util.parseDecimalInput

data class PositionEditUiState(
    val symbolId: Long,
    val ticker: String,
    val amountInput: String = "",
    val priceInput: String = "",
    /** Биржа, где куплена монета; `null` — не указана, оценка по лучшему bid. */
    val providerId: Int? = null,
    /** Биржи, на которых торгуется символ: из них и выбирают. */
    val exchanges: List<Provider> = emptyList(),
    /** Имя биржи сохранённой позиции — чтобы назвать её, даже если список не загрузился. */
    val savedExchangeName: String? = null,
    val showExchangePicker: Boolean = false,
    /** Позиция по символу уже есть: форма правит её, и появляется «Удалить». */
    val isExisting: Boolean = false,
    val isSaving: Boolean = false,
    /** Сохранили или удалили — экран закрывается. */
    val isDone: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val amount: Double? get() = amountInput.parseDecimalInput()
    val price: Double? get() = priceInput.parseDecimalInput()

    /** `null` — биржа не указана. Биржа без имени в справочнике называется своим id, как в профиле. */
    val exchangeName: String?
        get() =
            providerId?.let { id ->
                exchanges.firstOrNull { it.id == id }?.name?.takeIf { it.isNotBlank() }
                    ?: savedExchangeName
                    ?: id.toString()
            }

    /**
     * Ноль количества сохранять нечего: удаление — отдельная кнопка, а не
     * «сохранить пустое». Цена ноль допустима — так выглядят аирдропы.
     */
    val canSave: Boolean get() = !isSaving && (amount ?: 0.0) > 0.0 && price != null
}
