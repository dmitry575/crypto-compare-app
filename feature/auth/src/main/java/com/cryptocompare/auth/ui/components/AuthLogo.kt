package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textTertiary

/**
 * Знак приложения: два столбика разной высоты и разрыв между ними — цена одной
 * пары на двух биржах. Раньше здесь была плавающая ракета-эмодзи: эмодзи
 * рисуется шрифтом системы, на разных версиях Android выглядит по-разному
 * и к тому, чем занимается приложение, отношения не имеет.
 */
@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.textTertiary

    Canvas(modifier = modifier.size(Dimensions.IconSize.xxl)) {
        val barWidth = size.width * AuthConstants.Logo.BAR_WIDTH_RATIO
        val gap = size.width * AuthConstants.Logo.GAP_RATIO
        val radius = CornerRadius(barWidth / 2)
        val shortHeight = size.height * AuthConstants.Logo.SHORT_BAR_RATIO

        // левый столбик — цена ниже
        drawRoundRect(
            color = muted,
            topLeft = Offset(x = 0f, y = size.height - shortHeight),
            size = Size(width = barWidth, height = shortHeight),
            cornerRadius = radius,
        )

        // правый — выше: та же пара, другая биржа
        drawRoundRect(
            color = accent,
            topLeft = Offset(x = barWidth + gap, y = 0f),
            size = Size(width = barWidth, height = size.height),
            cornerRadius = radius,
        )
    }
}
