package com.cryptocompare.pairs.util

import com.cryptocompare.model.comparison.PairComparison
import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonCaptionTest {
    @Test
    fun `a small positive gap shows the difference, not a claim that selling pays less`() {
        // ethusdc 2026-09-15: купить на toobit 2498.45, продать на coinex 2499.17,
        // +0.03% — ниже порога подсветки, но в плюс
        assertEquals(
            ComparisonCaption.DIFFERENCE,
            ComparisonCaption.of(comparison(ask = 2498.45, bid = 2499.17, spread = 0.03)),
        )
    }

    @Test
    fun `a notable positive gap shows the difference`() {
        assertEquals(
            ComparisonCaption.DIFFERENCE,
            ComparisonCaption.of(comparison(ask = 100.0, bid = 101.0, spread = 1.0)),
        )
    }

    @Test
    fun `a negative gap says that selling pays less`() {
        assertEquals(
            ComparisonCaption.NO_PROFIT,
            ComparisonCaption.of(comparison(ask = 2606.08, bid = 2576.1, spread = -1.15)),
        )
    }

    @Test
    fun `equal prices show a zero difference rather than a loss`() {
        assertEquals(
            ComparisonCaption.DIFFERENCE,
            ComparisonCaption.of(comparison(ask = 100.0, bid = 100.0, spread = 0.0)),
        )
    }

    @Test
    fun `no best pair means no caption to compute`() {
        assertEquals(ComparisonCaption.NO_BEST, ComparisonCaption.of(comparison(ask = null, bid = null, spread = null)))
    }

    private fun comparison(
        ask: Double?,
        bid: Double?,
        spread: Double?,
    ) = PairComparison(
        ticker = "ethusdc",
        quotes = emptyList(),
        bestAskProviderId = 13,
        bestAskPrice = ask,
        bestBidProviderId = 14,
        bestBidPrice = bid,
        spreadPercent = spread,
        bestPrices = emptyList(),
    )
}
