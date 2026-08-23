package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    timeframe: ChartTimeframe,
    modifier: Modifier = Modifier,
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

    // шкалу Y считаем по видимым свечам, а не по всей загруженной истории:
    // иначе ось растянута на годовой размах, а видимый месяц — тонкая лента.
    // Чтение scrollState.value и zoomState.value подписывает нас на скролл
    // и пинч-зум: окно едет вместе с ними, у vico Zoom.x(N) держит в кадре N свечей.
    val window =
        visibleCandleWindow(
            candleCount = candles.size,
            visibleCount = zoomState.value.roundToInt(),
            scrollValue = scrollState.value,
            maxScrollValue = scrollState.maxValue,
        )

    // Транзакция перезапускается и при смене окна, хотя данные те же. Иначе новый
    // rangeProvider не доедет: внутри vico всё привязано к chart.id, а он переживает
    // rememberCartesianChart (там copy() исходного графика), так что границы
    // пересчитываются только на транзакции модели.
    LaunchedEffect(candles, window) {
        if (candles.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            candlestickSeries(
                x = candles.indices.toList(),
                opening = candles.map { it.open },
                closing = candles.map { it.close },
                low = candles.map { it.low },
                high = candles.map { it.high },
            )
        }
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
            modifier = modifier,
            // открываем на свежих свечах, а не на самом старом крае
            scrollState = scrollState,
            zoomState = zoomState,
            // данные при прокрутке не меняются, анимировать нечего
            animationSpec = null,
        )
    }
}

// внутри дня подписываем временем, на дневных/недельных — датой
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
