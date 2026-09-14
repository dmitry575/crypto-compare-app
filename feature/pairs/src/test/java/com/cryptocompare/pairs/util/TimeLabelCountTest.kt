package com.cryptocompare.pairs.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeLabelCountTest {
    @Test
    fun `labels at the chosen count never overlap`() {
        // перебираем ширины подписей от мелкого шрифта до крупного
        (40..400).forEach { labelWidth ->
            val plotWidth = 970f
            val count = timeLabelCount(plotWidth, labelWidth.toFloat())
            val step = plotWidth / (count + 1)

            if (count > 1) {
                assertTrue("шаг $step уже подписи $labelWidth при $count подписях", step >= labelWidth)
            }
        }
    }

    @Test
    fun `font scale 1_3 on a Pixel 8 gets three labels instead of four overlapping ones`() {
        // «29.07.26» при масштабе 1.3 — около 196px с отступами, ширина графика 970px:
        // раньше выходило четыре подписи с шагом 194px
        assertEquals(3, timeLabelCount(plotWidth = 970f, labelWidth = 196f))
    }

    @Test
    fun `normal font keeps the usual four labels`() {
        assertEquals(PairsConstants.Chart.TIME_LABEL_COUNT, timeLabelCount(plotWidth = 970f, labelWidth = 150f))
    }

    @Test
    fun `a very narrow chart still gets one label`() {
        assertEquals(1, timeLabelCount(plotWidth = 100f, labelWidth = 196f))
    }

    @Test
    fun `an unmeasured label does not divide by zero`() {
        assertEquals(PairsConstants.Chart.TIME_LABEL_COUNT, timeLabelCount(plotWidth = 970f, labelWidth = 0f))
    }
}
