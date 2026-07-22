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

        /** Сколько символов базовой валюты влезает в значок пары. */
        const val BADGE_MAX_CHARS = 4
        const val TICKER_PREFIX = "$"

        // размеры заглушек повторяют реальные строки, иначе список дёргается
        val skeletonTickerWidth: Dp = 96.dp
        val skeletonTickerHeight: Dp = 16.dp
        val skeletonSubtitleWidth: Dp = 56.dp
        val skeletonSubtitleHeight: Dp = 12.dp
        val skeletonPriceWidth: Dp = 72.dp
        val skeletonPriceHeight: Dp = 16.dp
    }

    object Chart {
        /** Запас по вертикали, чтобы свечи не упирались в края. */
        const val RANGE_PADDING = 0.05

        /**
         * Сколько свечей держим в кадре при открытии экрана: остальная история
         * листается вбок. Показать всю глубину сразу нельзя — год дневных свечей
         * на ширину телефона даёт по три пикселя на свечу.
         */
        const val VISIBLE_CANDLES = 60

        /**
         * Сколько подписей рисуем на оси цен. Дефолтный размещатель vico считает шаг
         * как 10^(floor(log10(maxY))-1) — то есть от порядка цены, а не от размаха.
         * Для ETH это шаг 100 при размахе 80, и на оси остаётся одна подпись.
         */
        const val PRICE_LABEL_COUNT = 6

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
