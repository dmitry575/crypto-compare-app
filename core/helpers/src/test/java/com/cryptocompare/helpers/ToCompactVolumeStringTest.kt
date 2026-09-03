package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToCompactVolumeStringTest {
    @Test
    fun `zero is a bare zero`() {
        assertEquals("0", 0.0.toCompactVolumeString())
    }

    @Test
    fun `values below a thousand keep their own scale`() {
        assertEquals("812.35", 812.345.toCompactVolumeString())
        assertEquals("0.5", 0.5.toCompactVolumeString())
    }

    @Test
    fun `thousands millions billions and trillions get a suffix`() {
        assertEquals("1.25K", 1_250.5.toCompactVolumeString())
        assertEquals("98.75M", 98_750_000.0.toCompactVolumeString())
        assertEquals("2.5B", 2_500_000_000.0.toCompactVolumeString())
        assertEquals("1.23T", 1_234_567_890_123.0.toCompactVolumeString())
    }

    @Test
    fun `round numbers lose trailing zeros`() {
        assertEquals("1M", 1_000_000.0.toCompactVolumeString())
        assertEquals("1K", 1_000.0.toCompactVolumeString())
    }

    @Test
    fun `rounding up moves the value to the next unit`() {
        // без перехода получилось бы «1000K», а это не короче исходного числа
        assertEquals("1M", 999_999.0.toCompactVolumeString())
        assertEquals("1B", 999_999_999.0.toCompactVolumeString())
    }

    @Test
    fun `sign survives`() {
        assertEquals("-1.25K", (-1_250.0).toCompactVolumeString())
    }

    @Test
    fun `non finite values fall back to a placeholder`() {
        assertEquals("—", Double.NaN.toCompactVolumeString())
        assertEquals("—", Double.POSITIVE_INFINITY.toCompactVolumeString())
    }
}
