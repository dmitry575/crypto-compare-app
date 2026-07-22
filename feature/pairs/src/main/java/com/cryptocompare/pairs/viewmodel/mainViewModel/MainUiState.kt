package com.cryptocompare.pairs.viewmodel.mainViewModel

data class MainUiState(
    val searchQuery: String = "",
    val error: String? = null,
    val subscribedTickers: Set<String> = emptySet(),
    val favouriteTickers: Set<String> = emptySet(),
    val onlyFavourite: Boolean = false,
)
