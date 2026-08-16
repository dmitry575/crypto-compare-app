package com.cryptocompare.helpers

import kotlin.math.abs

/**
 * Спред между покупкой и продажей в процентах — ширина рынка.
 *
 * Повторяет формулу SQL-агрегата `SymbolDao.pagingPairs()` слово в слово:
 * делим на меньшую цену. Пока формул было две (в списке `max − min` от `min`,
 * на детальном экране `bid − ask` от `ask`), одна и та же пара показывала
 * `1.16%` в списке и `-1.15%` на детальном экране.
 *
 * Всегда неотрицателен: аск не бывает ниже бида, а знак здесь ничего не значит.
 * Знак значим только у арбитража между биржами — там другая величина.
 */
fun spreadPercent(
    low: Double,
    high: Double,
): Double {
    if (low <= 0.0 || !low.isFinite() || !high.isFinite()) return 0.0
    return (high - low) * PERCENT_SCALE / low
}

/**
 * Арбитраж между биржами: купить по самому низкому аску, продать по самому
 * высокому биду. В отличие от [spreadPercent] знак значим — отрицательное
 * значение означает, что заработать на разнице нельзя.
 */
fun arbitragePercent(
    lowestAsk: Double,
    highestBid: Double,
): Double {
    if (lowestAsk <= 0.0 || !lowestAsk.isFinite() || !highestBid.isFinite()) return 0.0
    return (highestBid - lowestAsk) * PERCENT_SCALE / lowestAsk
}

/**
 * Bid-ask спред одной биржи в процентах: насколько аск выше бида, в долях от
 * самого аска этой биржи. Это отдельная величина — не путать со [spreadPercent]
 * (ширина рынка между биржами, делится на минимальную цену) и [arbitragePercent]
 * (заработок на перепродаже между биржами). Здесь знаменатель — аск конкретной
 * биржи, а значение всегда неотрицательно. Раньше эта формула жила прямо в
 * `ExchangeInfoCard` с `!!`, вне тестов.
 */
fun bidAskSpreadPercent(
    ask: Double,
    bid: Double,
): Double {
    if (ask <= 0.0 || !ask.isFinite() || !bid.isFinite()) return 0.0
    return abs(ask - bid) * PERCENT_SCALE / ask
}

private const val PERCENT_SCALE = 100.0
