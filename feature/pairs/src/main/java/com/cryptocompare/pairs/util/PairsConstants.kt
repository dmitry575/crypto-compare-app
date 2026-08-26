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
        /** Запас по вертикали, чтобы свечи не упирались в края кадра. */
        const val RANGE_PADDING = 0.05

        /**
         * Сколько свечей держим в кадре при открытии экрана: остальная история
         * листается вбок. Показать всю глубину сразу нельзя — год дневных свечей
         * на ширину телефона даёт по три пикселя на свечу.
         */
        const val VISIBLE_CANDLES = 60

        /** Границы пинч-зума: ближе десяти свечей колебания уже нечитаемы. */
        const val MIN_VISIBLE_CANDLES = 10

        /** Дальше этого отдалять нечего: свеча становится тоньше пикселя. */
        const val MAX_VISIBLE_CANDLES = 365

        /**
         * Глубина истории графика: столько свечей запрашиваем одним разом на пару
         * (биржа, масштаб). Ряд после загрузки не меняется, поэтому кадр не может
         * прыгнуть при догрузке — прыгать нечему. Потолок бэкенда — 1000; 500 это
         * ~8 экранов истории и ~50 КБ на запрос.
         */
        const val HISTORY_LIMIT = 500

        /**
         * Сколько подписей рисуем на оси цен. Дефолтный размещатель vico считает шаг
         * как 10^(floor(log10(maxY))-1) — то есть от порядка цены, а не от размаха.
         * Для ETH это шаг 100 при размахе 80, и на оси остаётся одна подпись.
         */
        const val PRICE_LABEL_COUNT = 6

        /**
         * Шаг оси цен округляем до «круглого» — 1/2/2.5/5 на порядок. Ось считается по
         * видимому кадру и пересчитывается на каждом кадре скролла: без округления
         * подписи дрожали бы непрерывно.
         */
        val NICE_STEP_MULTIPLIERS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)

        /**
         * Насколько (долей ширины кадра) должен сдвинуться кадр, чтобы пересчитать
         * по нему ось Y. Ниже порога сдвиг не отличим от дрожания ширины оси цен.
         */
        const val FRAME_UPDATE_RATIO = 0.05

        /** Размах окна оси цен, когда на видимом участке рынок стоит (high == low). */
        const val FLAT_SPAN_RATIO = 0.01

        /** Пол для тела свечи: при спокойном рынке иначе рисуется нить. */
        val minCandleBodyHeight: Dp = 2.dp

        /** Ниже этого значения время не похоже на epoch — подпись не строим. */
        const val EPOCH_LABEL_THRESHOLD = 1_000_000_000L

        /**
         * Подписи оси X. На дневном/недельном масштабе окно тянется на месяцы и годы,
         * поэтому в подписи нужен год. На внутридневных к времени добавляем дату —
         * иначе по одному «09:30» не понять, какой день. Формат компактный и не зависит
         * от локали (без названий месяцев).
         */
        const val LABEL_DATE_PATTERN = "dd.MM.yy"
        const val LABEL_TIME_PATTERN = "dd.MM HH:mm"

        /** Длительность одной свечи: по ней живой тик попадает в текущий бар. */
        const val M15_DURATION_MS = 15L * 60L * 1000L
        const val H1_DURATION_MS = 60L * 60L * 1000L
        const val H4_DURATION_MS = 4L * H1_DURATION_MS
        const val D1_DURATION_MS = 24L * H1_DURATION_MS
        const val W1_DURATION_MS = 7L * D1_DURATION_MS
    }

    object DetailScreen {
        const val SPREAD_FORMAT = "%.4f%%"

        /**
         * Тики приходят десятками в секунду; в состояние графика пробрасывается
         * только последний за интервал, иначе непрерывная рекомпозиция съедает кадры.
         */
        const val LIVE_PRICE_INTERVAL_MS = 500L

        val statusDotSize: Dp = 8.dp
        val websiteIconSize: Dp = 14.dp
        val labelGap: Dp = 2.dp
    }
}
