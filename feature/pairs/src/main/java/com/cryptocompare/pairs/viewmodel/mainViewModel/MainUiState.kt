package com.cryptocompare.pairs.viewmodel.mainViewModel

import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSorting

data class MainUiState(
    val searchQuery: String = "",
    val error: String? = null,
    val subscribedTickers: Set<String> = emptySet(),
    val favouriteTickers: Set<String> = emptySet(),
    val onlyFavourite: Boolean = false,
    /**
     * Направление за 24ч. Независимо от [onlyFavourite]: «избранное, которое
     * растёт» — это ровно тот вопрос, ради которого избранное и заводят.
     */
    val direction: CatalogDirection = CatalogDirection.ANY,
    val sorting: CatalogSorting = CatalogSorting(),
)
