package com.cryptocompare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.ui.theme.CryptoCompareThemePreview
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.ThemePreviews
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.textSecondary

/**
 * Чип-капсула: один вариант выбора в ленте.
 *
 * Этот вид был продублирован в выборе масштаба графика и в выборе биржи, а лента
 * фильтров каталога стала бы третьим местом — поэтому он здесь, а не в фиче.
 *
 * Слоты [leading] и [trailing] нужны, потому что содержимое чипа не сводится к
 * иконке с тинтом: у биржи слева цветная точка статуса, а у чипов, открывающих
 * шторку, справа будет счётчик или направление сортировки.
 *
 * Высота — [Dimensions.Height.chip], то есть 34dp против рекомендованных 48dp
 * для зоны нажатия. Это осознанно: ряд из капсул в 48dp читается как ряд кнопок
 * и перевешивает список, ради которого экран существует. Промах гасится
 * вертикальным зазором вокруг ленты.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Dimensions.Radius.full)
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier =
            modifier
                .heightIn(min = Dimensions.Height.chip)
                .background(
                    color = if (selected) accent else MaterialTheme.colorScheme.background,
                    shape = shape,
                ).border(
                    width = Dimensions.Border.thin,
                    color = if (selected) accent else MaterialTheme.colorScheme.borderPrimary,
                    shape = shape,
                )
                // clip до clickable, иначе рябь заливает прямоугольник поверх капсулы
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Dimensions.Padding.chipHorizontal,
                    vertical = Dimensions.Padding.chipVertical,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.textSecondary
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        trailing?.invoke()
    }
}

@ThemePreviews
@Composable
private fun AppFilterChipPreview() {
    CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme()) {
        AppChipRow(modifier = Modifier.padding(Dimensions.Padding.screen)) {
            AppFilterChip(label = "Все", selected = true, onClick = {})
            AppFilterChip(label = "Избранное", selected = false, onClick = {})
            AppFilterChip(
                label = "Binance",
                selected = false,
                onClick = {},
                leading = {
                    Box(
                        modifier =
                            Modifier
                                .size(Dimensions.IconSize.chipDot)
                                .background(MaterialTheme.colorScheme.statusActive, CircleShape),
                    )
                },
            )
        }
    }
}
