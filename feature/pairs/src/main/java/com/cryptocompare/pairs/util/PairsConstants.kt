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

        /** Разделитель между спредом и объёмом в приглушённом ряду строки. */
        const val META_SEPARATOR = " · "

        /**
         * Ниже этой ширины экрана приглушённый ряд не вмещает объём целиком, и
         * строка показывает только спред.
         *
         * Считано от раскладки строки: на 360dp левой колонке достаётся ~112dp,
         * а «0.42% · 98.75M» при 12sp занимает ~101dp. Запас в 11dp съедается
         * примерно на 344dp. Значение домножается на fontScale — при крупном
         * системном шрифте ряд растёт, а ширина экрана нет.
         */
        val volumeInRowMinWidth: Dp = 344.dp

        // размеры заглушек повторяют реальные строки, иначе список дёргается
        val skeletonTickerWidth: Dp = 96.dp
        val skeletonTickerHeight: Dp = 16.dp
        val skeletonSubtitleWidth: Dp = 56.dp
        val skeletonSubtitleHeight: Dp = 12.dp
        val skeletonPriceWidth: Dp = 72.dp
        val skeletonPriceHeight: Dp = 16.dp

        /** Заглушка под приглушённый ряд: спред и объём через разделитель. */
        val skeletonMetaWidth: Dp = 88.dp

        /** Заглушка под изменение за 24ч. */
        val skeletonChangeWidth: Dp = 48.dp
    }

    object ComparisonScreen {
        /** Тики идут десятками в секунду; в состояние уходит последний за интервал. */
        const val LIVE_PRICE_INTERVAL_MS = 500L

        /**
         * После скольких секунд котировка считается протухшей и приглушается.
         *
         * Повторяет `BestPriceMaxQuoteAgeSeconds` бэкенда: он с этим же порогом
         * не пускает такие котировки в лучшую пару. Разбивка по биржам приходит
         * **без** фильтра, поэтому на экране застрявшая цена выглядела бы самой
         * выгодной — на btcusdt 2026-09-08 такая стояла два часа.
         */
        const val STALE_QUOTE_SECONDS = 300L

        /** Колонки цен: шире имени биржи не нужны, а уже — обрезают число. */
        val priceColumnWidth: Dp = 88.dp

        /** Разделитель между объёмом и отметкой о несвежести. */
        const val META_SEPARATOR = " · "

        const val MILLIS_IN_SECOND = 1_000L
    }

    object Chart {
        /** Запас по вертикали, чтобы свечи не упирались в края кадра. */
        const val RANGE_PADDING = 0.05

        /**
         * Сколько свечей в кадре при открытии: остальная история листается вбок.
         * Показать всю глубину сразу нельзя — год дневных свечей на ширину телефона
         * даёт по три пикселя на свечу.
         */
        const val VISIBLE_CANDLES = 60

        /** Границы пинч-зума: ближе десяти свечей колебания уже нечитаемы. */
        const val MIN_VISIBLE_CANDLES = 10

        /** Дальше этого отдалять нечего: свеча становится тоньше пикселя. */
        const val MAX_VISIBLE_CANDLES = 365

        /**
         * Свечей за одну страницу истории. Ровно 100, потому что у части бирж
         * бэкенд больше за запрос не отдаёт — глубина набирается только страницами.
         */
        const val PAGE_LIMIT = 100

        /**
         * Потолок истории в памяти на пару (биржа, масштаб). Кеша на диске нет,
         * ряд растёт только влево и не выгружается, поэтому предел нужен.
         */
        const val MAX_CANDLES = 3000

        /**
         * За сколько экранов до края загруженного просим следующую страницу.
         * Запас нужен, чтобы страница доехала раньше, чем пользователь упрётся:
         * дальше загруженного края кадр не пускают, пустоты на графике не бывает.
         */
        const val PREFETCH_SCREENS = 1.0f

        /**
         * Сколько подписей рисуем на оси цен и на оси времени. Ось цен строится по
         * видимому кадру, поэтому шаг считается от размаха кадра, а не от порядка цены.
         */
        const val PRICE_LABEL_COUNT = 6
        const val TIME_LABEL_COUNT = 4

        /** Потолок числа подписей оси цен — страховка от кривого шага. */
        const val MAX_PRICE_LABELS = 12

        /**
         * Шаг оси цен округляем до «круглого» — 1/2/2.5/5 на порядок. Ось
         * пересчитывается на каждом кадре скролла: без округления подписи дрожали бы.
         */
        val NICE_STEP_MULTIPLIERS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)

        /** Размах окна оси цен, когда на видимом участке рынок стоит (high == low). */
        const val FLAT_SPAN_RATIO = 0.01

        /** Доля шага свечей, которую занимает тело; остальное — просвет между ними. */
        const val CANDLE_BODY_RATIO = 0.7f

        /** Пол для тела свечи: при спокойном рынке иначе рисуется нить. */
        val minCandleBodyHeight: Dp = 2.dp
        val candleWickWidth: Dp = 1.dp
        val gridLineWidth: Dp = 1.dp

        /** Отступ подписей осей от поля графика. */
        val axisLabelGap: Dp = 6.dp

        /** Индикатор догрузки более старой страницы у левого края графика. */
        val loadMoreIndicatorSize: Dp = 20.dp

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
        /**
         * Тики приходят десятками в секунду; в состояние графика пробрасывается
         * только последний за интервал, иначе непрерывная рекомпозиция съедает кадры.
         */
        const val LIVE_PRICE_INTERVAL_MS = 500L

        val statusDotSize: Dp = 8.dp
        val labelGap: Dp = 2.dp
    }
}
