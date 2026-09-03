package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToSignedPercentStringTest {
    @Test
    fun `growth carries an explicit plus`() {
        assertEquals("+2.35%", 2.35.toSignedPercentString())
    }

    @Test
    fun `decline carries a minus`() {
        assertEquals("-1.20%", (-1.2).toSignedPercentString())
    }

    @Test
    fun `a standing market has no sign at all`() {
        // «%+.2f» дал бы «+0.00%» и «-0.00%» для одного и того же стоящего рынка
        assertEquals("0.00%", 0.0.toSignedPercentString())
        assertEquals("0.00%", 0.001.toSignedPercentString())
        assertEquals("0.00%", (-0.001).toSignedPercentString())
    }

    @Test
    fun `non finite values fall back to a placeholder`() {
        assertEquals("—", Double.NaN.toSignedPercentString())
        assertEquals("—", Double.NEGATIVE_INFINITY.toSignedPercentString())
    }

    @Test
    fun `direction matches what the label shows`() {
        assertEquals(1, 2.35.priceChangeSign())
        assertEquals(-1, (-2.35).priceChangeSign())
        assertEquals(0, 0.001.priceChangeSign())
        assertEquals(0, Double.NaN.priceChangeSign())
    }
}
