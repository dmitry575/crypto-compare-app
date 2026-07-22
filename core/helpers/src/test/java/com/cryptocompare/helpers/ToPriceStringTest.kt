package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToPriceStringTest {
    @Test
    fun `large value keeps two decimals and strips trailing zeros`() {
        assertEquals("80145.3", 80145.300000000.toPriceString())
        assertEquals("65347.92", 65347.92.toPriceString())
    }

    @Test
    fun `whole large value has no decimals`() {
        assertEquals("80000", 80000.0.toPriceString())
    }

    @Test
    fun `mid range value keeps up to four decimals`() {
        assertEquals("46.7456", 46.7456.toPriceString())
        assertEquals("55.27", 55.27.toPriceString())
    }

    @Test
    fun `small value keeps significant figures instead of collapsing to zeros`() {
        assertEquals("0.00001684", 0.000016841.toPriceString())
        assertEquals("0.0000000034", 0.0000000034.toPriceString())
    }

    @Test
    fun `sub-unit value keeps significant figures`() {
        assertEquals("0.5", 0.5.toPriceString())
        assertEquals("0.1581", 0.1581.toPriceString())
    }

    @Test
    fun `zero renders as single zero`() {
        assertEquals("0", 0.0.toPriceString())
    }

    @Test
    fun `non-finite renders as dash`() {
        assertEquals("—", Double.NaN.toPriceString())
        assertEquals("—", Double.POSITIVE_INFINITY.toPriceString())
    }

    @Test
    fun `compact keeps short values plain`() {
        assertEquals("65549.99", 65549.99.toCompactPriceString())
        assertEquals("0.0006948", 0.0006948.toCompactPriceString())
        assertEquals("0.00000369", 0.00000369.toCompactPriceString())
        assertEquals("0", 0.0.toCompactPriceString())
    }

    @Test
    fun `compact switches very small values to scientific`() {
        assertEquals("3.31e-08", 0.00000003311.toCompactPriceString())
        assertEquals("4.24e-09", 0.000000004239.toCompactPriceString())
    }
}
