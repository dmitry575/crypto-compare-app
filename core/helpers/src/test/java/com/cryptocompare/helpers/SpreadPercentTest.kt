package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadPercentTest {
    /** Реальные цены CMETHUSDT, на которых расхождение и заметили. */
    private val bid = 2576.1
    private val ask = 2606.08

    @Test
    fun `spread matches the SQL aggregate`() {
        // SQL считает (max - min) * 100.0 / min — формула обязана совпадать,
        // иначе список и детальный экран снова разойдутся
        val expected = (ask - bid) * 100.0 / bid

        assertEquals(expected, spreadPercent(low = bid, high = ask), 1e-9)
    }

    @Test
    fun `spread is never negative`() {
        assertTrue(spreadPercent(low = bid, high = ask) > 0)
        // аргументы наоборот — вырожденный случай, но знака он давать не должен
        assertEquals(0.0, spreadPercent(low = ask, high = ask), 1e-9)
    }

    @Test
    fun `zero and broken prices do not blow up`() {
        assertEquals(0.0, spreadPercent(low = 0.0, high = 10.0), 1e-9)
        assertEquals(0.0, spreadPercent(low = -1.0, high = 10.0), 1e-9)
        assertEquals(0.0, spreadPercent(low = Double.NaN, high = 10.0), 1e-9)
        assertEquals(0.0, spreadPercent(low = 1.0, high = Double.NaN), 1e-9)
    }

    @Test
    fun `arbitrage keeps its sign`() {
        // одна биржа: бид ниже аска, заработать нельзя
        assertTrue(arbitragePercent(lowestAsk = ask, highestBid = bid) < 0)
        // разные биржи: бид одной выше аска другой
        assertTrue(arbitragePercent(lowestAsk = 1920.0, highestBid = 2361.0) > 0)
    }

    @Test
    fun `bid-ask spread divides by the ask`() {
        // формула карточки биржи: |ask - bid| / ask * 100 (знаменатель — аск,
        // в отличие от spreadPercent, где делим на минимум)
        val expected = (ask - bid) * 100.0 / ask

        assertEquals(expected, bidAskSpreadPercent(ask = ask, bid = bid), 1e-9)
    }

    @Test
    fun `bid-ask spread is never negative and survives broken prices`() {
        assertTrue(bidAskSpreadPercent(ask = ask, bid = bid) > 0)
        assertEquals(0.0, bidAskSpreadPercent(ask = 0.0, bid = 10.0), 1e-9)
        assertEquals(0.0, bidAskSpreadPercent(ask = -1.0, bid = 10.0), 1e-9)
        assertEquals(0.0, bidAskSpreadPercent(ask = Double.NaN, bid = 10.0), 1e-9)
        assertEquals(0.0, bidAskSpreadPercent(ask = 10.0, bid = Double.NaN), 1e-9)
    }

    @Test
    fun `the two measures no longer disagree on the same pair`() {
        // 1.16% против -1.15% — ровно то расхождение, из-за которого
        // и появились эти функции: величины разные, и путать их нельзя
        val width = spreadPercent(low = bid, high = ask)
        val arbitrage = arbitragePercent(lowestAsk = ask, highestBid = bid)

        assertEquals("1.16%", width.toPercentString())
        assertEquals("-1.15%", arbitrage.toPercentString())
    }
}
