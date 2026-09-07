package com.cryptocompare.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsSafeWebUrlTest {
    @Test
    fun `http and https pass`() {
        assertTrue("https://binance.com/register?ref=X1".isSafeWebUrl())
        assertTrue("http://example.com".isSafeWebUrl())
    }

    @Test
    fun `scheme is compared case insensitively`() {
        assertTrue("HTTPS://Binance.com".isSafeWebUrl())
    }

    @Test
    fun `surrounding whitespace does not break a valid link`() {
        assertTrue("  https://binance.com  ".isSafeWebUrl())
    }

    @Test
    fun `schemes that reach outside the browser are rejected`() {
        assertFalse("intent://scan/#Intent;scheme=zxing;end".isSafeWebUrl())
        assertFalse("javascript:alert(1)".isSafeWebUrl())
        assertFalse("file:///etc/passwd".isSafeWebUrl())
        assertFalse("content://com.example/secret".isSafeWebUrl())
        assertFalse("data:text/html,<script>".isSafeWebUrl())
    }

    @Test
    fun `a link without a scheme is not guessed at`() {
        // схему не дописываем: голый хост с бэкенда это баг, а не адрес
        assertFalse("example.com".isSafeWebUrl())
        assertFalse("//evil.com/x".isSafeWebUrl())
    }

    @Test
    fun `empty host is rejected`() {
        assertFalse("https://".isSafeWebUrl())
    }

    @Test
    fun `nothing at all is rejected`() {
        assertFalse(null.isSafeWebUrl())
        assertFalse("".isSafeWebUrl())
        assertFalse("   ".isSafeWebUrl())
        assertFalse("not a url at all".isSafeWebUrl())
    }
}
