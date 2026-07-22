package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToPercentStringTest {
    @Test
    fun `percent is rounded to two decimals`() {
        assertEquals("53.52%", 53.5235.toPercentString())
        assertEquals("0.19%", 0.1907.toPercentString())
    }

    @Test
    fun `exact zero stays zero`() {
        assertEquals("0.00%", 0.0.toPercentString())
    }

    @Test
    fun `tiny but non-zero spread does not collapse to zero`() {
        // 0.00% читалось бы как «разброса нет», хотя он есть
        assertEquals("<0.01%", 0.0004.toPercentString())
        assertEquals("-<0.01%", (-0.0004).toPercentString())
    }

    @Test
    fun `non-finite values get a placeholder`() {
        assertEquals("—", Double.NaN.toPercentString())
        assertEquals("—", Double.POSITIVE_INFINITY.toPercentString())
    }

    @Test
    fun `notable spread starts at a tenth of a percent`() {
        assertTrue(0.1.isNotableSpread())
        assertTrue(4.17.isNotableSpread())
        assertFalse(0.04.isNotableSpread())
    }

    @Test
    fun `notable spread ignores direction`() {
        assertTrue((-0.5).isNotableSpread())
    }
}
