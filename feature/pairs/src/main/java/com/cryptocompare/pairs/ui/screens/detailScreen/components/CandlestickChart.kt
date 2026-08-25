package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.chartNegative
import com.cryptocompare.ui.theme.chartPositive
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.VicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    timeframe: ChartTimeframe,
    modifier: Modifier = Modifier,
    onLoadOlder: () -> Unit = {},
    onLoadNewer: () -> Unit = {},
    canLoadOlder: Boolean = false,
    canLoadNewer: Boolean = false,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    // minZoom/maxZoom у vico ограничивают КОЭФФИЦИЕНТ зума, а он обратен числу видимых
    // свечей: Zoom.x(N) тем больше, чем меньше N. Поэтому нижней границе зума
    // соответствует максимум свечей в кадре, а верхней — минимум. Перепутать местами
    // нельзя: vico требует maxZoom >= minZoom и роняет экран на первом кадре.
    val zoomState =
        rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = Zoom.x(PairsConstants.Chart.VISIBLE_CANDLES.toDouble()),
            minZoom = Zoom.x(PairsConstants.Chart.MAX_VISIBLE_CANDLES.toDouble()),
            maxZoom = Zoom.x(PairsConstants.Chart.MIN_VISIBLE_CANDLES.toDouble()),
        )

    // ширину графика меряем для порога догрузки у краёв
    val viewportWidth = remember { mutableIntStateOf(0) }
    // прошлая раскладка — по ней при догрузке считаем сдвиг индексов, чтобы сохранить кадр
    val previousCandles = remember { mutableStateOf<List<Candle>?>(null) }
    // пока не встали на свежий край — не префетчим (иначе на открытии сразу грузим старое)
    val didInitialScroll = remember { mutableStateOf(false) }

    // Модель пересобираем ТОЛЬКО при смене данных — не на скролл/зум. Пересборка во
    // время жеста ломала зум, дёргала кадр и размазывала подписи оси Y.
    LaunchedEffect(candles) {
        if (candles.isEmpty()) return@LaunchedEffect

        val previous = previousCandles.value
        // на сколько сдвинулись индексы слева: + свечи дописаны слева, − левый край выгружен
        val shift = frontShift(previous, candles)
        val grew = previous != null && candles.size > previous.size
        previousCandles.value = candles
        val maxScrollBefore = scrollState.maxValue

        modelProducer.runTransaction {
            candlestickSeries(
                x = candles.indices.toList(),
                opening = candles.map { it.open },
                closing = candles.map { it.close },
                low = candles.map { it.low },
                high = candles.map { it.high },
            )
        }

        // догрузка слева / выгрузка края → сдвигаем кадр на число свечей, чтобы вид не
        // прыгнул (первую раскладку обрабатывает автоскролл к свежему краю). Если объём
        // вырос — ждём пересчёта предела скролла, иначе сдвиг упрётся в старый максимум.
        if (shift != 0 && didInitialScroll.value) {
            if (shift > 0 && grew) {
                withTimeoutOrNull(PairsConstants.Chart.SCROLL_ANCHOR_TIMEOUT_MS) {
                    snapshotFlow { scrollState.maxValue }.first { it > maxScrollBefore }
                }
            }
            scrollState.scroll(Scroll.Relative.x(shift.toDouble()))
        }
    }

    // встаём на свежий край после первой раскладки с данными: initialScroll=End мог
    // отработать ещё на пустой модели (данные приезжают асинхронно)
    LaunchedEffect(Unit) {
        withTimeoutOrNull(PairsConstants.Chart.SCROLL_ANCHOR_TIMEOUT_MS) {
            snapshotFlow { scrollState.maxValue }.first { it > 0f }
        }
        scrollState.scroll(Scroll.Absolute.End)
        didInitialScroll.value = true
    }

    // текущая ширина свечи в пикселях (учитывает зум) и видимый диапазон индексов —
    // по нему считаем и шкалу Y, и близость к краям окна для догрузки
    val contentWidth = scrollState.maxValue + viewportWidth.intValue.toFloat()
    val laidOut = viewportWidth.intValue > 0 && contentWidth > 0f && candles.isNotEmpty()
    val pxPerCandle = if (laidOut) contentWidth / candles.size else 0f
    val firstVisible =
        if (laidOut) (scrollState.value / pxPerCandle).toInt().coerceIn(0, candles.lastIndex) else 0
    val lastVisible =
        if (laidOut) {
            (firstVisible + (viewportWidth.intValue / pxPerCandle).toInt() + 1)
                .coerceIn(firstVisible, candles.lastIndex)
        } else {
            candles.lastIndex.coerceAtLeast(0)
        }

    // догрузку у краёв ловим по позиции скролла. Запас — несколько свечей от края (в
    // пикселях текущего зума), а НЕ целый экран: иначе на коротком ряде «у края»
    // оказывается сразу весь кадр и страницы грузятся без остановки.
    val prefetchMargin = pxPerCandle * PairsConstants.Chart.PREFETCH_MARGIN_CANDLES
    val atOldEdge = laidOut && didInitialScroll.value && scrollState.value <= prefetchMargin
    val atNewEdge =
        laidOut && didInitialScroll.value && scrollState.value >= scrollState.maxValue - prefetchMargin

    LaunchedEffect(canLoadOlder && atOldEdge) {
        if (canLoadOlder && atOldEdge) onLoadOlder()
    }
    LaunchedEffect(canLoadNewer && atNewEdge) {
        if (canLoadNewer && atNewEdge) onLoadNewer()
    }

    // Ось Y — по видимому кадру, а не по всему окну: иначе далёкая по цене свеча из
    // глубины (памп/дамп на 10x) сплющивает текущие свечи в тонкую ленту. У vico
    // rangeProvider применяется только на транзакции модели (гонять её на каждый скролл
    // нельзя — ломается зум), поэтому шкала подстраивается под кадр при смене данных,
    // в т.ч. при догрузке страницы. Без fixed ось Y начинается с нуля.
    val rangeProvider =
        remember(candles, firstVisible, lastVisible) {
            val slice = candles.subList(firstVisible, (lastVisible + 1).coerceAtMost(candles.size))
            if (slice.isEmpty()) {
                CartesianLayerRangeProvider.auto()
            } else {
                val low = slice.minOf { it.low }
                val high = slice.maxOf { it.high }
                val padding =
                    ((high - low) * PairsConstants.Chart.RANGE_PADDING)
                        .takeIf { it > 0.0 } ?: (high * PairsConstants.Chart.RANGE_PADDING)
                CartesianLayerRangeProvider.fixed(minY = low - padding, maxY = high + padding)
            }
        }

    val priceFormatter = remember { CartesianValueFormatter { _, value, _ -> value.toPriceString() } }
    val priceItemPlacer =
        remember { VerticalAxis.ItemPlacer.count({ PairsConstants.Chart.PRICE_LABEL_COUNT }) }
    val timeFormatter =
        remember(candles, timeframe) {
            val labelFormat = SimpleDateFormat(timeframe.labelPattern(), Locale.US)
            CartesianValueFormatter { _, value, _ ->
                candles.getOrNull(value.toInt())?.let { candle ->
                    formatCandleLabel(candle.timeMillis, labelFormat)
                } ?: ""
            }
        }

    // Без своей темы vico берёт палитру по isSystemInDarkTheme(), а не по выбору
    // пользователя. Цвета берём из colorScheme — он уже учитывает выбор.
    val chartTheme =
        rememberM3VicoTheme(
            candlestickCartesianLayerColors =
                VicoTheme.CandlestickCartesianLayerColors(
                    bullish = MaterialTheme.colorScheme.chartPositive,
                    neutral = MaterialTheme.colorScheme.textTertiary,
                    bearish = MaterialTheme.colorScheme.chartNegative,
                ),
            lineColor = MaterialTheme.colorScheme.outline,
            textColor = MaterialTheme.colorScheme.textSecondary,
        )

    ProvideVicoTheme(chartTheme) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberCandlestickCartesianLayer(
                        rangeProvider = rangeProvider,
                        minCandleBodyHeight = PairsConstants.Chart.minCandleBodyHeight,
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            valueFormatter = priceFormatter,
                            itemPlacer = priceItemPlacer,
                        ),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                ),
            modelProducer = modelProducer,
            modifier = modifier.onSizeChanged { viewportWidth.intValue = it.width },
            scrollState = scrollState,
            zoomState = zoomState,
            // данные при прокрутке не меняются, анимировать нечего
            animationSpec = null,
        )
    }
}

// внутри дня подписываем датой и временем, на дневных/недельных — датой с годом
private fun ChartTimeframe.labelPattern(): String =
    when (this) {
        ChartTimeframe.M15, ChartTimeframe.H1, ChartTimeframe.H4 -> PairsConstants.Chart.LABEL_TIME_PATTERN
        ChartTimeframe.D1, ChartTimeframe.W1 -> PairsConstants.Chart.LABEL_DATE_PATTERN
    }

private fun formatCandleLabel(
    timeMillis: Long,
    format: SimpleDateFormat,
): String =
    if (timeMillis > PairsConstants.Chart.EPOCH_LABEL_THRESHOLD) {
        format.format(Date(timeMillis))
    } else {
        ""
    }

// сдвиг левого края окна между раскладками: >0 — свечи дописаны слева (догрузка
// старой страницы), <0 — левый край выгружен (окно поехало к настоящему)
private fun frontShift(
    previous: List<Candle>?,
    current: List<Candle>,
): Int {
    if (previous.isNullOrEmpty() || current.isEmpty()) return 0

    val prevFirstTime = previous.first().timeMillis
    val idxInCurrent = current.indexOfFirst { it.timeMillis == prevFirstTime }
    if (idxInCurrent >= 0) return idxInCurrent

    // прежняя первая свеча выгружена слева — считаем, сколько свечей ушло
    val currentFirstTime = current.first().timeMillis
    val idxInPrevious = previous.indexOfFirst { it.timeMillis == currentFirstTime }
    return if (idxInPrevious >= 0) -idxInPrevious else 0
}
