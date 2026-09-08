package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadPercentTest {
    /** Реальные цены CMETHUSDT, на которых расхождение и заметили. */
    private val bid = 2576.1
    private val ask = 2606.08

    @Test
    fun `formula repeats the backend`() {
        // серверный spreadPercent = (priceSell - priceBuy) / priceBuy * 100,
        // где priceBuy — цена покупки. Сверено на живых данных: btcusdt
        // 78955.42 / 78886.40 даёт 0.0875%, и бэкенд отдаёт ровно его
        val expected = (78_955.42 - 78_886.40) * 100.0 / 78_886.40

        assertEquals(expected, spreadPercent(buyPrice = 78_886.40, sellPrice = 78_955.42)!!, 1e-9)
        assertEquals("0.09%", spreadPercent(buyPrice = 78_886.40, sellPrice = 78_955.42)!!.toPercentString())
    }

    @Test
    fun `sign is kept and is negative in the normal case`() {
        // купить по аску, продать по биду — обычное состояние рынка,
        // и оно должно читаться как минус, а не как возможность
        val value = spreadPercent(buyPrice = ask, sellPrice = bid)!!

        assertTrue(value < 0)
        assertEquals("-1.15%", value.toPercentString())
    }

    @Test
    fun `positive value means a real opportunity`() {
        assertTrue(spreadPercent(buyPrice = 1_920.0, sellPrice = 2_361.0)!! > 0)
    }

    @Test
    fun `a single exchange degenerates to its own bid-ask`() {
        // при одной бирже цены покупки и продажи — её собственные аск и бид,
        // отдельной формулы для этого случая заводить не нужно
        val single = spreadPercent(buyPrice = ask, sellPrice = bid)

        assertEquals((bid - ask) * 100.0 / ask, single!!, 1e-9)
    }

    @Test
    fun `equal prices give exactly zero`() {
        assertEquals(0.0, spreadPercent(buyPrice = ask, sellPrice = ask)!!, 1e-9)
    }

    @Test
    fun `missing and broken prices give null, not zero`() {
        // ноль означал бы «спреда нет», а это другое утверждение
        assertNull(spreadPercent(buyPrice = null, sellPrice = 10.0))
        assertNull(spreadPercent(buyPrice = 10.0, sellPrice = null))
        assertNull(spreadPercent(buyPrice = 0.0, sellPrice = 10.0))
        assertNull(spreadPercent(buyPrice = 10.0, sellPrice = 0.0))
        assertNull(spreadPercent(buyPrice = -1.0, sellPrice = 10.0))
        assertNull(spreadPercent(buyPrice = Double.NaN, sellPrice = 10.0))
        assertNull(spreadPercent(buyPrice = 10.0, sellPrice = Double.NaN))
        assertNull(spreadPercent(buyPrice = 10.0, sellPrice = Double.POSITIVE_INFINITY))
    }
}
