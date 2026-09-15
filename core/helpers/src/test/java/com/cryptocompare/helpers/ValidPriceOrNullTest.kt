package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidPriceOrNullTest {
    @Test
    fun `a positive finite price is kept as is`() {
        assertEquals(0.03156, 0.03156.validPriceOrNull()!!, 0.0)
    }

    @Test
    fun `zero means the side is missing`() {
        assertNull(0.0.validPriceOrNull())
    }

    @Test
    fun `negative price is not a price`() {
        assertNull((-1.0).validPriceOrNull())
    }

    @Test
    fun `non finite values are dropped`() {
        // бесконечность больше нуля, поэтому одной проверки знака мало
        assertNull(Double.POSITIVE_INFINITY.validPriceOrNull())
        assertNull(Double.NaN.validPriceOrNull())
    }

    @Test
    fun `null stays null`() {
        assertNull((null as Double?).validPriceOrNull())
    }
}
