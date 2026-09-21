package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToSignedPriceStringTest {
    @Test
    fun `profit gets a plus, loss keeps its own minus`() {
        assertEquals("+345.67", 345.67.toSignedPriceString())
        assertEquals("-12.3", (-12.3).toSignedPriceString())
    }

    @Test
    fun `zero has no sign`() {
        // «+0» у позиции, которая стоит столько же, сколько за неё заплатили,
        // читался бы как округлённая прибыль
        assertEquals("0", 0.0.toSignedPriceString())
    }

    @Test
    fun `small numbers keep their significant digits`() {
        assertEquals("+0.0000003311", 0.0000003311.toSignedPriceString())
    }

    @Test
    fun `a non-number stays a dash, without a sign`() {
        assertEquals("—", Double.NaN.toSignedPriceString())
        assertEquals("—", Double.POSITIVE_INFINITY.toSignedPriceString())
    }
}
