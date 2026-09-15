package com.cryptocompare.pairs.util

import com.cryptocompare.model.comparison.PairComparison

/**
 * Что написать слева от процента в выжимке сравнения.
 *
 * Выбор идёт по **знаку** разницы, а не по порогу подсветки. Раньше «продать
 * дешевле, чем купить» писалось для всего, что не дотянуло до порога 0.1%, — и
 * при разнице +0.03% экран утверждал обратное тому, что показывал: продажа там
 * дороже покупки. Порог решает только, красить ли процент акцентом.
 */
internal enum class ComparisonCaption {
    /** Бэкенд лучшую пару не прислал — и числа, и подписи нет. */
    NO_BEST,

    /** Продажа не дешевле покупки: показываем разницу в котируемом активе. */
    DIFFERENCE,

    /** Продажа дешевле покупки — обычное состояние рынка, говорим словами. */
    NO_PROFIT,
    ;

    companion object {
        fun of(comparison: PairComparison): ComparisonCaption {
            val difference = comparison.difference

            return when {
                comparison.spreadPercent == null || difference == null -> NO_BEST
                difference < 0 -> NO_PROFIT
                else -> DIFFERENCE
            }
        }
    }
}
