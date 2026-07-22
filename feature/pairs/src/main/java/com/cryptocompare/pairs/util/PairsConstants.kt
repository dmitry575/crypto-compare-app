package com.cryptocompare.pairs.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Константы фичи pairs. Размеры, у которых есть смысловой аналог в
 * [com.cryptocompare.ui.theme.Dimensions], сюда не дублируются.
 */
object PairsConstants {
    object Navigation {
        const val TICKER_ARG = "ticker"
    }

    object MainScreen {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val PRICE_FLUSH_INTERVAL_MS = 500L

        /** Колонка цен шире колонки тикера: мелкие цены длиннее названий. */
        const val PRICE_COLUMN_WEIGHT = 1.5f
        const val TICKER_COLUMN_WEIGHT = 1f

        val skeletonTickerHeight: Dp = 20.dp
        val skeletonPriceHeight: Dp = 18.dp
    }

    object Chart {
        /** Запас по вертикали, чтобы свечи не упирались в края. */
        const val RANGE_PADDING = 0.05

        /** Пол для тела свечи: при спокойном рынке иначе рисуется нить. */
        val minCandleBodyHeight: Dp = 2.dp

        /** Ниже этого значения время не похоже на epoch — подпись не строим. */
        const val EPOCH_LABEL_THRESHOLD = 1_000_000_000L

        /** Внутри дня подписывать датой бессмысленно — она везде одинаковая. */
        const val LABEL_DATE_PATTERN = "MM-dd"
        const val LABEL_TIME_PATTERN = "HH:mm"
    }

    object DetailScreen {
        const val SPREAD_FORMAT = "%.4f%%"

        val statusDotSize: Dp = 8.dp
        val websiteIconSize: Dp = 14.dp
        val labelGap: Dp = 2.dp
    }
}
