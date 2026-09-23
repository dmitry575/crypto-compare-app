package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ToPairNameTest {
    @Test
    fun `a raw ticker becomes base slash quote`() {
        assertEquals("BTC/USDT", "btcusdt".toPairName())
        assertEquals("ETH/BTC", "ETHBTC".toPairName())
    }

    @Test
    fun `other separators are brought to a slash`() {
        assertEquals("BTC/USDT", "btc-usdt".toPairName())
        assertEquals("BTC/USDT", "btc_usdt".toPairName())
        assertEquals("BTC/USDT", "BTC/USDT".toPairName())
    }

    @Test
    fun `an unknown quote is left whole, not cut in half`() {
        // «XYZ/ABC» было бы выдумкой: котируемой валюты ABC в словаре нет
        assertEquals("XYZABC", "xyzabc".toPairName())
    }

    @Test
    fun `a bare quote currency is not split into nothing`() {
        assertEquals("USDT", "usdt".toPairName())
    }
}
