package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.AppConstants

/**
 * Название пары для заголовков и строк: «BTC/USDT» вместо сырого «btcusdt».
 *
 * Разбор тот же, что у строки каталога ([parseTicker]): пара должна называться
 * одинаково везде, где её видно. Раньше каталог писал «BTC/USDT», а экран пары,
 * сравнение и портфель — «BTCUSDT», и казалось, что это разные вещи.
 *
 * Тикер, который не разобрался (незнакомая котируемая валюта), остаётся как есть,
 * заглавными: «XYZABC» честнее, чем придуманное деление посередине.
 */
fun String.toPairName(): String =
    parseTicker()?.let { (base, quote) -> base + AppConstants.PAIR_SEPARATOR + quote } ?: uppercase()
