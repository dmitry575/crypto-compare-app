package com.cryptocompare.helpers

import com.cryptocompare.model.ticker.TickerBestPrice

// Правило выбора лучшей пары — одно на всё приложение: загрузку и живые события
// сокета, экран деталей и экран сравнения. Разойдись они, блок разницы на деталях
// и выжимка сравнения показали бы для одной пары разные числа, а после первого же
// тика — не то, что при открытии экрана.

/**
 * У пары есть обе стороны и обе биржи. Каталог отбрасывает неполные строки тем
 * же правилом, в `NormalizeSymbols`.
 */
fun TickerBestPrice.isComplete(): Boolean =
    bestAskProviderId != null &&
        bestBidProviderId != null &&
        bestAskPrice.validPriceOrNull() != null &&
        bestBidPrice.validPriceOrNull() != null

/**
 * Самая широкая из лучших пар тикера. Символов у тикера бывает несколько — на
 * 2026-09-14 так у 310 тикеров из 2857, — и смысл приложения в том, чтобы
 * показать, где разница между биржами больше всего. Каталог берёт тот же
 * `MAX(spreadPercent)`.
 */
fun List<TickerBestPrice>.widest(): TickerBestPrice? = maxByOrNull { it.spreadPercent ?: Double.NEGATIVE_INFINITY }

/**
 * Лучшие пары после событий сокета: событие заменяет строку своего символа,
 * символ, увиденный впервые, добавляется. Неполное событие пропускается целиком —
 * брать из него то, что есть, значит склеить биржу из нового события с ценой
 * из старого.
 */
fun List<TickerBestPrice>.withUpdates(updates: Collection<TickerBestPrice>): List<TickerBestPrice> {
    val complete = updates.filter { it.isComplete() }
    if (complete.isEmpty()) return this

    return (associateBy { it.symbolId } + complete.associateBy { it.symbolId }).values.toList()
}
