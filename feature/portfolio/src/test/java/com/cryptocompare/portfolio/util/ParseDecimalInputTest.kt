package com.cryptocompare.portfolio.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseDecimalInputTest {
    @Test
    fun `a comma and a dot both mean the fraction`() {
        // русская раскладка даёт запятую, скопированная с биржи цена — точку
        assertEquals(0.42, "0,42".parseDecimalInput()!!, DELTA)
        assertEquals(0.42, "0.42".parseDecimalInput()!!, DELTA)
    }

    @Test
    fun `digit grouping is ignored`() {
        assertEquals(72_000.0, "72 000".parseDecimalInput()!!, DELTA)
        assertEquals(72_000.5, "72 000,5".parseDecimalInput()!!, DELTA)
        assertEquals(1_000_000.0, "1 000 000".parseDecimalInput()!!, DELTA)
    }

    @Test
    fun `an empty field is not a number`() {
        assertNull("".parseDecimalInput())
        assertNull("   ".parseDecimalInput())
    }

    @Test
    fun `garbage is not guessed at`() {
        // форма не должна угадывать, что имелось в виду
        assertNull("abc".parseDecimalInput())
        assertNull("1.2.3".parseDecimalInput())
        assertNull("1,2.3".parseDecimalInput())
        assertNull("1e5".parseDecimalInput())
    }

    @Test
    fun `negative numbers are refused`() {
        assertNull("-1".parseDecimalInput())
    }

    @Test
    fun `a trailing separator is still a number`() {
        // пользователь посреди набора: «0,» — это уже ноль, а не ошибка
        assertEquals(0.0, "0,".parseDecimalInput()!!, DELTA)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}
