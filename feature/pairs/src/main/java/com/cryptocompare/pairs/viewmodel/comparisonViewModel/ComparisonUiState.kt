package com.cryptocompare.pairs.viewmodel.comparisonViewModel

import com.cryptocompare.model.comparison.PairComparison

data class ComparisonUiState(
    val ticker: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val comparison: PairComparison? = null,
) {
    /** Нечего показывать: ни одной биржи с котировкой. */
    val isEmpty: Boolean
        get() = !loading && comparison?.quotes.isNullOrEmpty()
}
