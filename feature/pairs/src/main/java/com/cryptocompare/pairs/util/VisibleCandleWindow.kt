package com.cryptocompare.pairs.util

import kotlin.math.roundToInt

/**
 * Индексы свечей, попадающих в кадр при текущем скролле.
 *
 * Нужны, чтобы шкала Y считалась по видимому окну, а не по всей загруженной
 * истории: иначе при годе дневных свечей ось растянута на весь годовой размах,
 * а видимый месяц вырождается в тонкую ленту. У vico `getVisibleXRange`
 * internal, поэтому окно вычисляем сами.
 *
 * Число видимых свечей приходит извне, а не считается из пикселей: график
 * открывается с `Zoom.x(visibleCount)` и щипковый зум выключен, поэтому в кадре
 * всегда ровно [visibleCount] свечей. Из пикселей берём только долю прокрутки —
 * от ширины полосы оси Y она не зависит, так что обратной связи «шире подписи →
 * уже кадр → другое окно → другие подписи» не возникает.
 *
 * @param candleCount сколько свечей загружено
 * @param visibleCount сколько свечей помещается в кадр
 * @param scrollValue текущая позиция скролла в пикселях
 * @param maxScrollValue предел скролла в пикселях; 0 — вся история влезла в кадр
 */
internal fun visibleCandleWindow(
    candleCount: Int,
    visibleCount: Int,
    scrollValue: Float,
    maxScrollValue: Float,
): IntRange {
    if (candleCount <= 0) return IntRange.EMPTY

    val visible = visibleCount.coerceIn(1, candleCount)
    // листать нечего: вся история и так в кадре
    if (maxScrollValue <= 0f) return 0..candleCount - 1

    val scrolled = (scrollValue / maxScrollValue).coerceIn(0f, 1f)
    val first =
        ((candleCount - visible) * scrolled)
            .roundToInt()
            .coerceIn(0, candleCount - visible)

    // по свече с каждого края: та, что срезана границей кадра, видна наполовину,
    // и её хвост тоже должен попадать в диапазон
    return (first - 1).coerceAtLeast(0)..(first + visible).coerceAtMost(candleCount - 1)
}
