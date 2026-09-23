package com.cryptocompare.pairs.viewmodel.comparisonViewModel

import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.model.error.AppError

data class ComparisonUiState(
    val ticker: String = "",
    val loading: Boolean = true,
    val error: AppError? = null,
    val comparison: PairComparison? = null,
) {
    /** Нечего показывать: ни одной биржи с котировкой. */
    val isEmpty: Boolean
        get() = !loading && comparison?.quotes.isNullOrEmpty()
}
