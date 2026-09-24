package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class FormatLastUpdateTest {
    @Test
    fun `today is shown as time`() {
        val updated = millis(LocalDateTime.of(2026, 9, 17, 13, 48))
        val now = millis(LocalDateTime.of(2026, 9, 17, 19, 5))

        assertEquals("13:48", formatLastUpdate(updated, now, ZONE))
    }

    @Test
    fun `just past midnight is still time, not a date`() {
        val updated = millis(LocalDateTime.of(2026, 9, 17, 0, 3))
        val now = millis(LocalDateTime.of(2026, 9, 17, 0, 4))

        assertEquals("00:03", formatLastUpdate(updated, now, ZONE))
    }

    @Test
    fun `yesterday is shown as a date`() {
        // «13:48» на вчерашних данных выглядело бы свежим
        val updated = millis(LocalDateTime.of(2026, 9, 16, 13, 48))
        val now = millis(LocalDateTime.of(2026, 9, 17, 0, 10))

        assertEquals("16.09", formatLastUpdate(updated, now, ZONE))
    }

    @Test
    fun `older data is shown as a date too`() {
        val updated = millis(LocalDateTime.of(2026, 3, 2, 9, 0))
        val now = millis(LocalDateTime.of(2026, 9, 17, 19, 5))

        assertEquals("02.03", formatLastUpdate(updated, now, ZONE))
    }

    private fun millis(dateTime: LocalDateTime): Long = dateTime.atZone(ZONE).toInstant().toEpochMilli()

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    }
}
