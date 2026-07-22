package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.ui.theme.chartNegative
import com.cryptocompare.ui.theme.chartPositive

/**
 * Свечи теми же цветами, что и настоящий график: зелёный и красный в приложении
 * означают только направление цены, и на онбординге тоже.
 *
 * Форма зашита константами, а не случайна: иллюстрация должна выглядеть одинаково
 * на каждом запуске, иначе она читается как живые данные, которых здесь нет.
 */
@Composable
internal fun CandlesArt(modifier: Modifier = Modifier) {
    val up = MaterialTheme.colorScheme.chartPositive
    val down = MaterialTheme.colorScheme.chartNegative

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(AuthConstants.Onboarding.artCandlesHeight),
    ) {
        val candles = AuthConstants.Onboarding.SAMPLE_CANDLES
        val slot = size.width / candles.size
        val bodyWidth = slot * AuthConstants.Onboarding.CANDLE_BODY_RATIO
        val radius = CornerRadius(bodyWidth / 4)

        candles.forEachIndexed { index, candle ->
            val (top, bottom, rising) = candle
            val centerX = slot * index + slot / 2
            val color = if (rising) up else down

            // фитиль
            drawRoundRect(
                color = color,
                topLeft =
                    Offset(
                        x = centerX - AuthConstants.Onboarding.CANDLE_WICK_WIDTH / 2,
                        y = size.height * (top - AuthConstants.Onboarding.CANDLE_WICK_OVERHANG),
                    ),
                size =
                    Size(
                        width = AuthConstants.Onboarding.CANDLE_WICK_WIDTH,
                        height =
                            size.height *
                                (bottom - top + 2 * AuthConstants.Onboarding.CANDLE_WICK_OVERHANG),
                    ),
            )

            // тело
            drawRoundRect(
                color = color,
                topLeft = Offset(x = centerX - bodyWidth / 2, y = size.height * top),
                size = Size(width = bodyWidth, height = size.height * (bottom - top)),
                cornerRadius = radius,
            )
        }
    }
}
