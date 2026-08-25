package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.visibleCandleWindow
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
import kotlin.math.roundToInt

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    onLoadOlder: () -> Unit = {},
    onLoadNewer: () -> Unit = {},
    canLoadOlder: Boolean = false,
    canLoadNewer: Boolean = false,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    // minZoom/maxZoom у vico ограничивают КОЭФФИЦИЕНТ зума, а он обратен числу
    // видимых свечей: Zoom.x(N) тем больше, чем меньше N. Поэтому нижней границе
    // зума соответствует максимум свечей в кадре, а верхней — минимум. Перепутать
    // местами нельзя: vico требует maxZoom >= minZoom и роняет экран на первом кадре.
    val zoomState =
        rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = Zoom.x(PairsConstants.Chart.VISIBLE_CANDLES.toDouble()),
            minZoom = Zoom.x(PairsConstants.Chart.MAX_VISIBLE_CANDLES.toDouble()),
            maxZoom = Zoom.x(PairsConstants.Chart.MIN_VISIBLE_CANDLES.toDouble()),
        )

    // ширину области графика меряем сами — по ней и пределу скролла считаем, сколько
    // свечей реально в кадре. zoomState.value — это КОЭФФИЦИЕНТ зума, а не число
    // свечей, брать его напрямую нельзя: округляется к ~1, и ось Y подгоняется под
    // одну свечу, а остальные уходят за кадр.
    val viewportWidth = remember { mutableIntStateOf(0) }

    // шкалу Y считаем по видимым свечам, а не по всей загруженной истории: иначе ось
    // растянута на весь размах окна, а видимый месяц — тонкая лента. Чтение
    // scrollState.value/maxValue подписывает нас и на скролл, и на пинч-зум (зум
    // меняет предел скролла), так что окно едет вместе с ними.
    val window =
        visibleCandleWindow(
            candleCount = candles.size,
            visibleCount = visibleCandleCount(candles.size, viewportWidth.intValue.toFloat(), scrollState.maxValue),
            scrollValue = scrollState.value,
            maxScrollValue = scrollState.maxValue,
        )

    // прошлая раскладка свечей: по ней ловим сдвиг окна (догрузку слева или выгрузку
    // края) и компенсируем скролл, чтобы под пальцем осталась та же свеча
    val previousCandles = remember { mutableStateOf<List<Candle>?>(null) }

    // Транзакция перезапускается и при смене окна, хотя данные те же. Иначе новый
    // rangeProvider не доедет: внутри vico всё привязано к chart.id, а он переживает
    // rememberCartesianChart (там copy() исходного графика), так что границы
    // пересчитываются только на транзакции модели.
    LaunchedEffect(candles, window) {
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

        // сдвигаем кадр на число свечей сдвига, чтобы вид не прыгнул. Relative.x меряет
        // в единицах X — не зависит от зума и ширины оси. Если свечи прибавились слева
        // и объём вырос, ждём пересчёта предела скролла, иначе сдвиг упрётся в старый
        // максимум; при выгрузке края или в равновесии окна ждать нечего.
        if (shift != 0) {
            if (shift > 0 && grew) {
                withTimeoutOrNull(PairsConstants.Chart.SCROLL_ANCHOR_TIMEOUT_MS) {
                    snapshotFlow { scrollState.maxValue }.first { it > maxScrollBefore }
                }
            }
            scrollState.scroll(Scroll.Relative.x(shift.toDouble()))
        }
    }

    // встаём на свежий край после первой раскладки с данными. initialScroll=End у vico
    // мог отработать ещё на пустой модели (данные приезжают асинхронно), и график
    // открывался бы на самом старом крае. Отдельный одноразовый эффект, чтобы его не
    // отменяла перекладка окна.
    LaunchedEffect(Unit) {
        withTimeoutOrNull(PairsConstants.Chart.SCROLL_ANCHOR_TIMEOUT_MS) {
            snapshotFlow { scrollState.maxValue }.first { it > 0f }
        }
        scrollState.scroll(Scroll.Absolute.End)
    }

    // у краёв окна просим соседнюю страницу — с запасом, чтобы она успела приехать,
    // пока пользователь не упёрся в пустоту
    val shouldLoadOlder = canLoadOlder && window.first <= PairsConstants.Chart.LOAD_MORE_MARGIN
    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) onLoadOlder()
    }

    val shouldLoadNewer = canLoadNewer && window.last >= candles.size - 1 - PairsConstants.Chart.LOAD_MORE_MARGIN
    LaunchedEffect(shouldLoadNewer) {
        if (shouldLoadNewer) onLoadNewer()
    }

    // без этого ось Y начинается с нуля и свечи сплющиваются в полоску сверху
    val rangeProvider =
        remember(candles, window) {
            val visible = candles.slice(window)
            if (visible.isEmpty()) {
                CartesianLayerRangeProvider.auto()
            } else {
                val low = visible.minOf { it.low }
                val high = visible.maxOf { it.high }
                val padding =
                    ((high - low) * PairsConstants.Chart.RANGE_PADDING)
                        .takeIf { it > 0.0 } ?: (high * PairsConstants.Chart.RANGE_PADDING)
                CartesianLayerRangeProvider.fixed(minY = low - padding, maxY = high + padding)
            }
        }

    val priceFormatter = remember { CartesianValueFormatter { _, value, _ -> value.toPriceString() } }
    val priceItemPlacer =
        remember { VerticalAxis.ItemPlacer.count({ PairsConstants.Chart.PRICE_LABEL_COUNT }) }
    // подписи оси X тирами: время внутри дня, дата на смене дня, дата с годом на
    // смене года — тир выбираем сравнением с предыдущей свечой
    val timeFormatter =
        remember(candles) {
            val dayKey = SimpleDateFormat(PairsConstants.Chart.LABEL_DAY_KEY_PATTERN, Locale.US)
            val dateWithYear = SimpleDateFormat(PairsConstants.Chart.LABEL_DATE_PATTERN, Locale.US)
            val dateNoYear = SimpleDateFormat(PairsConstants.Chart.LABEL_DAY_PATTERN, Locale.US)
            val time = SimpleDateFormat(PairsConstants.Chart.LABEL_TIME_PATTERN, Locale.US)
            CartesianValueFormatter { _, value, _ ->
                val index = value.toInt()
                val candle = candles.getOrNull(index) ?: return@CartesianValueFormatter ""
                formatTieredLabel(candle, candles.getOrNull(index - 1), dayKey, dateWithYear, dateNoYear, time)
            }
        }

    // постоянная дата левого края кадра — как на биржах, чтобы день и год были видны
    // всегда, даже когда весь кадр внутри одного дня (на оси тогда только время)
    val anchorFormat = remember { SimpleDateFormat(PairsConstants.Chart.LABEL_DATE_PATTERN, Locale.US) }
    val anchorDate =
        remember(candles, window.first) {
            candles.getOrNull(window.first)?.let { anchorFormat.format(Date(it.timeMillis)) }
        }

    // Без своей темы vico берёт палитру по isSystemInDarkTheme(), а не по выбору
    // пользователя: при системной светлой и тёмной теме в приложении оси рисовались
    // почти чёрным по тёмному фону. Цвета берём из colorScheme — он уже учитывает выбор.
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
        Box(modifier = modifier) {
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
                // меряем ширину графика — по ней считаем число видимых свечей для шкалы Y
                modifier = Modifier.fillMaxSize().onSizeChanged { viewportWidth.intValue = it.width },
                // открываем на свежих свечах, а не на самом старом крае
                scrollState = scrollState,
                zoomState = zoomState,
                // данные при прокрутке не меняются, анимировать нечего
                animationSpec = null,
            )

            if (anchorDate != null) {
                Text(
                    text = anchorDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textSecondary,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(PairsConstants.Chart.axisDateLabelPadding),
                )
            }
        }
    }
}

// сколько свечей реально помещается в кадр: доля ширины кадра во всём контенте,
// умноженная на число свечей. Ширину берём всего хоста (с полосой оси Y) — она чуть
// больше самой области графика, поэтому окно выходит с запасом и видимые свечи в
// шкалу Y попадают все, а не режутся. Хост фиксирован разметкой, так что обратной
// связи «шире подписи → уже кадр → другое окно» не возникает.
private fun visibleCandleCount(
    candleCount: Int,
    viewportWidth: Float,
    maxScrollValue: Float,
): Int {
    if (candleCount <= 0) return 0
    // ещё не разложено — берём дефолт открытия
    if (viewportWidth <= 0f) return PairsConstants.Chart.VISIBLE_CANDLES
    // весь ряд влез в кадр — листать нечего
    if (maxScrollValue <= 0f) return candleCount

    val fraction = viewportWidth / (maxScrollValue + viewportWidth)
    return (candleCount * fraction).roundToInt().coerceIn(1, candleCount)
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

// тир подписи по сравнению с предыдущей свечой: другой год → дата с годом, другой
// день → дата без года, тот же день → время. Ключ дня — «yyyyMMdd», год в нём первые
// 4 символа. Так на оси не повторяется полная дата, а контекст виден на границах.
private fun formatTieredLabel(
    candle: Candle,
    previous: Candle?,
    dayKey: SimpleDateFormat,
    dateWithYear: SimpleDateFormat,
    dateNoYear: SimpleDateFormat,
    time: SimpleDateFormat,
): String {
    if (candle.timeMillis <= PairsConstants.Chart.EPOCH_LABEL_THRESHOLD) return ""
    val date = Date(candle.timeMillis)
    if (previous == null || previous.timeMillis <= PairsConstants.Chart.EPOCH_LABEL_THRESHOLD) {
        return dateWithYear.format(date)
    }

    val currentKey = dayKey.format(date)
    val previousKey = dayKey.format(Date(previous.timeMillis))
    return when {
        currentKey.take(4) != previousKey.take(4) -> dateWithYear.format(date)
        currentKey != previousKey -> dateNoYear.format(date)
        else -> time.format(date)
    }
}
