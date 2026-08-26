package com.cryptocompare.pairs.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleXRangeItemPlacerTest {
    @Test
    fun `the first frame is always reported`() {
        assertTrue(null.differsNoticeablyFrom(0.0..60.0))
    }

    @Test
    fun `a shift smaller than the threshold is ignored`() {
        // ширина оси цен меняется на знак — кадр дёргается на доли процента,
        // и пересчитывать по этому ось нельзя: получится петля
        val previous = 0.0..60.0

        assertFalse(previous.differsNoticeablyFrom(0.0..61.0))
        assertFalse(previous.differsNoticeablyFrom((-1.0)..60.0))
    }

    @Test
    fun `scrolling past the threshold is reported`() {
        val previous = 0.0..60.0

        assertTrue(previous.differsNoticeablyFrom(4.0..64.0))
    }

    @Test
    fun `zooming out is reported even with the frame start intact`() {
        val previous = 0.0..60.0

        assertTrue(previous.differsNoticeablyFrom(0.0..200.0))
    }

    @Test
    fun `the threshold scales with the frame so it works at any zoom`() {
        // тот же сдвиг в 4 единицы: на широком кадре это дрожание, на узком — скролл
        val wide = 0.0..365.0
        val narrow = 0.0..10.0

        assertFalse(wide.differsNoticeablyFrom(4.0..369.0))
        assertTrue(narrow.differsNoticeablyFrom(4.0..14.0))
    }

    @Test
    fun `a collapsed frame is reported instead of dividing by nothing`() {
        assertTrue((0.0..60.0).differsNoticeablyFrom(5.0..5.0))
    }
}
