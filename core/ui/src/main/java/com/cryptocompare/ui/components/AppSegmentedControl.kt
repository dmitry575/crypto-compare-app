package com.cryptocompare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.bgSunk
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textSecondary

/**
 * Выбор одного из нескольких равнозначных вариантов: фильтр каталога, тема
 * в профиле. Нужен двум фичам, поэтому живёт здесь.
 *
 * Выбранный сегмент подсвечивается не акцентом, а поднятием на поверхность:
 * акцент в приложении означает действие, а тут пользователь просто отмечает,
 * что смотрит. Так ряд сегментов не спорит за внимание с кнопками.
 */
@Composable
fun <T> AppSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.Radius.md))
                .background(MaterialTheme.colorScheme.bgSunk)
                .padding(Dimensions.Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.bgCard
                    } else {
                        MaterialTheme.colorScheme.bgSunk
                    },
                label = "segmentBackground",
            )

            Text(
                text = label(option),
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.textPrimary
                    } else {
                        MaterialTheme.colorScheme.textSecondary
                    },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimensions.Radius.sm))
                        .background(background)
                        .clickable { onSelect(option) }
                        .padding(vertical = Dimensions.Spacing.xs),
            )
        }
    }
}
