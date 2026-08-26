package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.VisibleXRangeItemPlacer
import com.cryptocompare.pairs.util.chartPriceRange
import com.cryptocompare.ui.theme.chartNegative
import com.cryptocompare.ui.theme.chartPositive
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CandlestickCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
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

/**
 * Свечной график одной биржи. Ряд свечей неизменен всё время жизни графика —
 * история приходит одним запросом, — поэтому кадру не от чего прыгать: двигается
 * только последний, живой бар. Ось Y подстраивается под видимый кадр.
 */
@Composable
fun CandlestickChart(
    candles: List<Candle>,
    timeframe: ChartTimeframe,
    modifier: Modifier = Modifier,
) {
    if (candles.isEmpty()) return

    // Модель собираем сами, без CartesianChartModelProducer: он применяет данные
    // асинхронной транзакцией и только в ней пересчитывает диапазоны осей. Ось Y
    // из-за этого отставала от кадра, и на отъехавшем участке свечи уходили за её
    // границы — график выглядел пустым. Прямая модель пересчитывает диапазоны в
    // той же композиции, что и смена данных либо кадра.
    val model = remember(candles) { candles.toCandlestickModel() }

    // видимый диапазон X отдаёт сам vico при отрисовке подписей оси
    var visibleXRange by remember { mutableStateOf<ClosedFloatingPointRange<Double>?>(null) }
    val timeItemPlacer =
        remember {
            VisibleXRangeItemPlacer(HorizontalAxis.ItemPlacer.aligned()) { visibleXRange = it }
        }

    val priceRange = remember(candles, visibleXRange) { candles.chartPriceRange(visibleXRange) }
    val priceStep = priceRange.step
    val rangeProvider =
        remember(priceRange) {
            CartesianLayerRangeProvider.fixed(minY = priceRange.min, maxY = priceRange.max)
        }
    // шаг задаём сами: подписи тогда стоят на кратных ему круглых ценах и не
    // пересчитываются на каждом кадре скролла
    val priceItemPlacer = remember(priceStep) { VerticalAxis.ItemPlacer.step({ priceStep }) }

    // Границы зума ОБЯЗАТЕЛЬНО запоминаем: Zoom.x(...) — фабрика, каждый вызов даёт
    // новый объект, а rememberVicoZoomState держит границы ключами rememberSaveable.
    // Незапомненные — состояние зума пересоздаётся на каждой рекомпозиции (живой бар
    // рекомпозит график дважды в секунду) и откатывается к начальному: со стороны это
    // и выглядит как «зум не работает». Zoom.max(..., Zoom.Content) не даёт отдалиться
    // дальше, чем влезает весь ряд, — иначе короткая история сжимается в угол.
    val zoomState =
        rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = remember { visibleCandlesZoom(PairsConstants.Chart.VISIBLE_CANDLES) },
            minZoom = remember { visibleCandlesZoom(PairsConstants.Chart.MAX_VISIBLE_CANDLES) },
            maxZoom = remember { visibleCandlesZoom(PairsConstants.Chart.MIN_VISIBLE_CANDLES) },
        )
    // те же ключи-ловушки: у rememberVicoScrollState дефолтный spring() создаётся
    // заново на каждый вызов. Автоскролла тут нет, спек нужен лишь стабильный
    val scrollState =
        rememberVicoScrollState(
            initialScroll = Scroll.Absolute.End,
            autoScrollAnimationSpec = remember { snap() },
        )

    val priceFormatter = remember { CartesianValueFormatter { _, value, _ -> value.toPriceString() } }
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
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            valueFormatter = timeFormatter,
                            itemPlacer = timeItemPlacer,
                        ),
                ),
            model = model,
            modifier = modifier,
            scrollState = scrollState,
            zoomState = zoomState,
        )
    }
}

// x свечи — её индекс в ряду: по времени немаркетные периоды растянули бы ось пустотой
private fun List<Candle>.toCandlestickModel(): CartesianChartModel =
    CartesianChartModel(
        listOf(
            CandlestickCartesianLayerModel
                .partial(
                    opening = map { it.open },
                    closing = map { it.close },
                    low = map { it.low },
                    high = map { it.high },
                ).complete(),
        ),
    )

/**
 * Зум, при котором в кадре [candles] свечей, но не мельче, чем весь ряд целиком:
 * Zoom.x задаёт КОЭФФИЦИЕНТ, и на истории короче запрошенного кадра он уводит
 * свечи в угол пустого поля.
 */
private fun visibleCandlesZoom(candles: Int): Zoom = Zoom.max(Zoom.x(candles.toDouble()), Zoom.Content)

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
