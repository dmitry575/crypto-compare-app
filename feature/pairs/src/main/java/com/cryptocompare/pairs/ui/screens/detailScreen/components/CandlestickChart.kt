package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.chart.Candle
import com.cryptocompare.model.chart.ChartTimeframe
import com.cryptocompare.pairs.util.ChartPriceRange
import com.cryptocompare.pairs.util.ChartViewport
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.chartPriceRange
import com.cryptocompare.pairs.util.clampedTo
import com.cryptocompare.pairs.util.freshEdgeViewport
import com.cryptocompare.pairs.util.needsOlderPage
import com.cryptocompare.pairs.util.pinnedToFreshEdge
import com.cryptocompare.pairs.util.priceLabels
import com.cryptocompare.pairs.util.scrolledBy
import com.cryptocompare.pairs.util.visibleIndices
import com.cryptocompare.pairs.util.zoomedBy
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.chartNegative
import com.cryptocompare.ui.theme.chartPositive
import com.cryptocompare.ui.theme.divider
import com.cryptocompare.ui.theme.textSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Свечной график выбранной биржи: свой рендер на Canvas вместо готовой библиотеки.
 *
 * Причина не в рисовании, а в пагинации. Библиотеки графиков меряют скролл в
 * пикселях от начала содержимого, поэтому дописанная слева страница истории
 * сдвигает кадр на свою ширину, а компенсировать сдвиг в том же кадре нечем —
 * прыжок видно всегда. Здесь кадр ([ChartViewport]) считается в свечах и в
 * абсолютных индексах, которые от догрузки не меняются: страница дописывается, а
 * кадр стоит на месте сам собой. Отсюда же свои границы скролла (за загруженное не
 * уехать, пустоты не бывает) и понятный зум — [ChartViewport.visibleCount] это
 * прямо число свечей в кадре, а не коэффициент.
 *
 * @param liveCount сколько баров в хвосте дорисовал живой тик: по ним считается
 *   абсолютный индекс, см. [ChartViewport].
 */
@Composable
fun CandlestickChart(
    candles: List<Candle>,
    liveCount: Int,
    timeframe: ChartTimeframe,
    canLoadOlder: Boolean,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candles.isEmpty()) return

    val serverCount = candles.size - liveCount
    val oldestAbs = 1 - serverCount
    val newestAbs = liveCount

    var viewport by remember {
        mutableStateOf(freshEdgeViewport(newestAbs).clampedTo(oldestAbs, newestAbs))
    }
    // ширину поля графика знает только отрисовка (её съедает ось цен), а жесту она
    // нужна, чтобы переводить пиксели пальца в свечи
    var plotLeftPx by remember { mutableFloatStateOf(0f) }
    var plotWidthPx by remember { mutableFloatStateOf(0f) }

    // жест живёт дольше композиции, поэтому свежие границы и колбэк берём через State
    val currentOldest by rememberUpdatedState(oldestAbs)
    val currentNewest by rememberUpdatedState(newestAbs)
    val currentCanLoadOlder by rememberUpdatedState(canLoadOlder)
    val currentOnLoadOlder by rememberUpdatedState(onLoadOlder)

    fun moveViewport(next: ChartViewport) {
        viewport = next.clampedTo(currentOldest, currentNewest)
        if (currentCanLoadOlder && viewport.needsOlderPage(currentOldest)) currentOnLoadOlder()
    }

    // Страница приехала или живой тик открыл новый бар. Кадр при догрузке не трогаем —
    // индексы абсолютные, — но за живым краем следуем, если стояли на нём. Здесь же
    // решается открытие: первой страницы на экран с запасом не хватает, поэтому
    // следующая просится сразу, не дожидаясь жеста.
    LaunchedEffect(oldestAbs, newestAbs, canLoadOlder) {
        val followedFreshEdge = viewport.rightEdge >= newestAbs
        viewport =
            (if (followedFreshEdge) viewport.pinnedToFreshEdge(newestAbs) else viewport)
                .clampedTo(oldestAbs, newestAbs)
        if (canLoadOlder && viewport.needsOlderPage(oldestAbs)) onLoadOlder()
    }

    val positiveColor = MaterialTheme.colorScheme.chartPositive
    val negativeColor = MaterialTheme.colorScheme.chartNegative
    val gridColor = MaterialTheme.colorScheme.divider
    val labelStyle = NumericType.Caption.copy(color = MaterialTheme.colorScheme.textSecondary)
    val textMeasurer = rememberTextMeasurer()
    val timeFormat = remember(timeframe) { SimpleDateFormat(timeframe.labelPattern(), Locale.US) }

    // инерция после броска: без неё до истории годичной давности пришлось бы
    // добираться десятком свайпов
    val decaySpec = rememberSplineBasedDecay<Float>()
    val flingScope = rememberCoroutineScope()
    var fling by remember { mutableStateOf<Job?>(null) }

    Canvas(
        modifier =
            modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fling?.cancel()

                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    var totalDx = 0f
                    var totalDy = 0f
                    var claimed = false
                    var abandoned = false
                    var perCandle = plotWidthPx.takeIf { it > 0f } ?: size.width.toFloat()

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        val plotWidth = plotWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        perCandle = plotWidth / viewport.visibleCount

                        if (pressed.size >= 2) {
                            // щипок: свеча под центром жеста остаётся на месте
                            val focus = ((event.calculateCentroid().x - plotLeftPx) / plotWidth).coerceIn(0f, 1f)
                            val panned = viewport.scrolledBy(-event.calculatePan().x / perCandle)
                            moveViewport(panned.zoomedBy(event.calculateZoom(), focus))
                            event.changes.forEach { if (it.pressed) it.consume() }
                            claimed = true
                            continue
                        }
                        if (abandoned) continue

                        val change = pressed.first()
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y
                        if (!claimed) {
                            // вертикаль отдаём экрану: график лежит в прокручиваемой колонке
                            if (abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)) {
                                abandoned = true
                                continue
                            }
                            if (abs(totalDx) > viewConfiguration.touchSlop) claimed = true
                        }
                        if (claimed) {
                            velocity.addPosition(change.uptimeMillis, change.position)
                            moveViewport(viewport.scrolledBy(-delta.x / perCandle))
                            change.consume()
                        }
                    }

                    if (claimed && perCandle > 0f) {
                        val candlesPerSecond = -velocity.calculateVelocity().x / perCandle
                        fling = flingScope.launch { flingBy(candlesPerSecond, decaySpec, ::moveViewport) { viewport } }
                    }
                }
            },
    ) {
        val visible = viewport.visibleIndices(candles.size, serverCount)
        if (visible.isEmpty()) return@Canvas

        // ось цен — по видимому кадру: одна свеча из глубины истории иначе
        // сплющивает текущие в тонкую ленту
        val range = candles.subList(visible.first, visible.last + 1).chartPriceRange()
        val labels = range.priceLabels()
        val layouts = labels.map { textMeasurer.measure(it.toPriceString(), labelStyle) }
        val gap = PairsConstants.Chart.axisLabelGap.toPx()

        // поле графика: слева ось цен с отступом по обе стороны, снизу ось времени
        val left = layouts.maxOf { it.size.width } + gap * 2
        val bottom = size.height - (layouts.first().size.height + gap)
        val width = size.width - left
        if (width <= 0f || bottom <= 0f) return@Canvas

        drawPriceAxis(labels, layouts, range, left, bottom, gridColor)
        drawTimeAxis(candles, viewport, serverCount, left, width, bottom, gridColor, textMeasurer, labelStyle) {
            formatCandleLabel(it, timeFormat)
        }
        clipRect(left = left, top = 0f, right = size.width, bottom = bottom) {
            drawCandles(
                candles = candles,
                visible = visible,
                viewport = viewport,
                serverCount = serverCount,
                range = range,
                plotLeft = left,
                plotWidth = width,
                plotBottom = bottom,
                positiveColor = positiveColor,
                negativeColor = negativeColor,
            )
        }

        if (plotLeftPx != left) plotLeftPx = left
        if (plotWidthPx != width) plotWidthPx = width
    }
}

/**
 * Инерция после броска: кадр продолжает ехать и гаснет по той же кривой, что и
 * системный скролл. Упёрлись в край загруженного — гасим сразу, чтобы палец не
 * ждал окончания анимации впустую.
 */
private suspend fun flingBy(
    candlesPerSecond: Float,
    decaySpec: DecayAnimationSpec<Float>,
    move: (ChartViewport) -> Unit,
    current: () -> ChartViewport,
) {
    if (!candlesPerSecond.isFinite() || candlesPerSecond == 0f) return

    var last = 0f
    AnimationState(initialValue = 0f, initialVelocity = candlesPerSecond).animateDecay(decaySpec) {
        val before = current().leftEdge
        move(current().scrolledBy(value - last))
        last = value
        if (current().leftEdge == before) cancelAnimation()
    }
}

/** Y-координата цены: верх поля — [ChartPriceRange.max], низ — [ChartPriceRange.min]. */
private fun ChartPriceRange.yOf(
    price: Double,
    plotBottom: Float,
): Float {
    val span = (max - min).takeIf { it > 0.0 } ?: return plotBottom
    return (plotBottom * (1.0 - (price - min) / span)).toFloat()
}

/** Ось цен слева и горизонтальная сетка по её подписям. */
private fun DrawScope.drawPriceAxis(
    labels: List<Double>,
    layouts: List<TextLayoutResult>,
    range: ChartPriceRange,
    plotLeft: Float,
    plotBottom: Float,
    gridColor: Color,
) {
    val lineWidth = PairsConstants.Chart.gridLineWidth.toPx()
    val gap = PairsConstants.Chart.axisLabelGap.toPx()

    labels.forEachIndexed { index, price ->
        val y = range.yOf(price, plotBottom)
        drawLine(gridColor, Offset(plotLeft, y), Offset(size.width, y), lineWidth)

        val layout = layouts[index]
        // крайние подписи прижимаем к полю, иначе они срезаются краем графика
        val top = (y - layout.size.height / 2f).coerceIn(0f, plotBottom - layout.size.height)
        drawText(layout, topLeft = Offset(plotLeft - gap - layout.size.width, top))
    }
}

/**
 * Ось времени под графиком и вертикальная сетка по её подписям. Число подписей
 * подгоняется под их ширину: на внутридневном масштабе подпись вдвое длиннее
 * («24.08 12:00» против «20.03.26»), и четыре штуки налезали бы друг на друга.
 */
private fun DrawScope.drawTimeAxis(
    candles: List<Candle>,
    viewport: ChartViewport,
    serverCount: Int,
    plotLeft: Float,
    plotWidth: Float,
    plotBottom: Float,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    format: (Long) -> String,
) {
    val perCandle = plotWidth / viewport.visibleCount
    val lineWidth = PairsConstants.Chart.gridLineWidth.toPx()
    val gap = PairsConstants.Chart.axisLabelGap.toPx()

    fun candleAt(absIndex: Float) = candles.getOrNull(absIndex.roundToInt() + serverCount - 1)

    val sample = candleAt(viewport.leftEdge + viewport.visibleCount / 2) ?: return
    val sampleWidth = textMeasurer.measure(format(sample.timeMillis), labelStyle).size.width + gap * 2
    val count =
        (plotWidth / sampleWidth)
            .toInt()
            .coerceIn(1, PairsConstants.Chart.TIME_LABEL_COUNT)
    val stepCandles = viewport.visibleCount / (count + 1)

    for (step in 1..count) {
        val absIndex = viewport.leftEdge + stepCandles * step
        val candle = candleAt(absIndex) ?: continue
        val text = format(candle.timeMillis)
        if (text.isEmpty()) continue

        val x = plotLeft + (absIndex - viewport.leftEdge) * perCandle
        drawLine(gridColor, Offset(x, 0f), Offset(x, plotBottom), lineWidth)

        val layout = textMeasurer.measure(text, labelStyle)
        val labelLeft = (x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
        drawText(layout, topLeft = Offset(labelLeft, plotBottom + gap))
    }
}

/** Сами свечи: фитиль от high к low, тело между open и close. */
private fun DrawScope.drawCandles(
    candles: List<Candle>,
    visible: IntRange,
    viewport: ChartViewport,
    serverCount: Int,
    range: ChartPriceRange,
    plotLeft: Float,
    plotWidth: Float,
    plotBottom: Float,
    positiveColor: Color,
    negativeColor: Color,
) {
    val perCandle = plotWidth / viewport.visibleCount
    val bodyWidth = (perCandle * PairsConstants.Chart.CANDLE_BODY_RATIO).coerceAtLeast(1f)
    val wickWidth = PairsConstants.Chart.candleWickWidth.toPx()
    val minBodyHeight = PairsConstants.Chart.minCandleBodyHeight.toPx()

    for (index in visible) {
        val candle = candles[index]
        val absIndex = index - (serverCount - 1)
        val center = plotLeft + (absIndex - viewport.leftEdge + 0.5f) * perCandle
        val color = if (candle.close >= candle.open) positiveColor else negativeColor

        drawLine(
            color = color,
            start = Offset(center, range.yOf(candle.high, plotBottom)),
            end = Offset(center, range.yOf(candle.low, plotBottom)),
            strokeWidth = wickWidth,
        )

        val bodyTop = range.yOf(max(candle.open, candle.close), plotBottom)
        val bodyBottom = range.yOf(min(candle.open, candle.close), plotBottom)
        drawRect(
            color = color,
            topLeft = Offset(center - bodyWidth / 2f, bodyTop),
            size = Size(bodyWidth, (bodyBottom - bodyTop).coerceAtLeast(minBodyHeight)),
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
