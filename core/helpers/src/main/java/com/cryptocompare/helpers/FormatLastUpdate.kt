package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.TimeFormatConstants
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Когда цены на экране обновлялись в последний раз.
 *
 * Сегодняшнее время — часами и минутами: «13:48» отвечает на вопрос «насколько
 * это старое» одним взглядом. Всё, что старше сегодняшнего дня, показывается
 * датой: «13:48» на позавчерашних данных выглядело бы свежим.
 */
fun formatLastUpdate(
    millis: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val updated = Instant.ofEpochMilli(millis).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    val pattern =
        if (updated.toLocalDate() == today) {
            TimeFormatConstants.LAST_UPDATE_TIME_PATTERN
        } else {
            TimeFormatConstants.LAST_UPDATE_DATE_PATTERN
        }

    return DateTimeFormatter.ofPattern(pattern, Locale.ROOT).format(updated)
}
