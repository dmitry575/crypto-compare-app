package com.cryptocompare.pairs.viewmodel.mainViewModel

import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSorting

data class MainUiState(
    val searchQuery: String = "",
    val error: String? = null,
    val subscribedTickers: Set<String> = emptySet(),
    /** Избранное — по символам: у тикера их столько, сколько наборов сетей. */
    val favouriteSymbolIds: Set<Long> = emptySet(),
    val onlyFavourite: Boolean = false,
    /**
     * Гость попросил избранное. Экран показывает приглашение войти и гасит
     * флаг: это разовое событие, а не состояние.
     */
    val signInRequired: Boolean = false,
    /**
     * Направление за 24ч. Независимо от [onlyFavourite]: «избранное, которое
     * растёт» — это ровно тот вопрос, ради которого избранное и заводят.
     */
    val direction: CatalogDirection = CatalogDirection.ANY,
    val sorting: CatalogSorting = CatalogSorting(),
)
