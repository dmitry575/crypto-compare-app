package com.cryptocompare.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cryptocompare.ui.theme.Dimensions

/**
 * Горизонтальная лента чипов.
 *
 * Одна на весь проект, чтобы шаг между капсулами и поведение прокрутки не
 * разъезжались между экранами. Лента, а не ряд равных сегментов: подписи у
 * фильтров разной длины, и растягивать «Все» до ширины «Избранного» незачем,
 * а при крупном системном шрифте ряд просто прокручивается дальше.
 */
@Composable
fun AppChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
