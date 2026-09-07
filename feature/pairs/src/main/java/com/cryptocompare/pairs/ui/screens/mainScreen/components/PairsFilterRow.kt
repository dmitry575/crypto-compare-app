package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.components.AppChipRow
import com.cryptocompare.ui.components.AppFilterChip
import com.cryptocompare.ui.theme.CryptoCompareThemePreview
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.ThemePreviews

/**
 * Быстрые фильтры каталога.
 *
 * Два независимых измерения, а не один список из четырёх вариантов: направление
 * выбирается одно из трёх, избранное включается отдельно. Иначе «избранное,
 * которое сегодня растёт» — тот самый вопрос, ради которого избранное и заводят, —
 * было бы не задать.
 *
 * Лента чипов, а не сегментированный контрол: тот делит ширину поровну между
 * вариантами, и на четырёх подписи начинали обрезаться уже при обычном шрифте.
 * Чипы меряются по содержимому, лишнее уезжает за край прокруткой — сюда же
 * встанут чип выбора бирж и чип сортировки.
 */
@Composable
internal fun PairsFilterRow(
    direction: CatalogDirection,
    onDirectionChange: (CatalogDirection) -> Unit,
    onlyFavourite: Boolean,
    onOnlyFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // прокручивается только группа направления: она и будет расти, когда сюда
        // приедут выбор бирж и сортировка
        AppChipRow(modifier = Modifier.weight(1f)) {
            CatalogDirection.entries.forEach { entry ->
                AppFilterChip(
                    label = stringResource(entry.labelRes()),
                    selected = entry == direction,
                    onClick = { onDirectionChange(entry) },
                )
            }
        }

        // избранное — другое измерение, поэтому стоит вне ленты и прижато к краю:
        // внутри прокрутки его к правому краю не прижать, а болтающийся в середине
        // ряда чип читается как незаполненное место
        AppFilterChip(
            label = stringResource(R.string.pairs_filter_favorites),
            selected = onlyFavourite,
            onClick = { onOnlyFavouriteChange(!onlyFavourite) },
            leading = {
                Icon(
                    imageVector = if (onlyFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.IconSize.sm),
                )
            },
        )
    }
}

private fun CatalogDirection.labelRes(): Int =
    when (this) {
        CatalogDirection.ANY -> R.string.pairs_filter_all
        CatalogDirection.GAINERS -> R.string.pairs_filter_gainers
        CatalogDirection.LOSERS -> R.string.pairs_filter_losers
    }

@ThemePreviews
@Composable
private fun PairsFilterRowPreview() {
    CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme()) {
        PairsFilterRow(
            direction = CatalogDirection.GAINERS,
            onDirectionChange = {},
            onlyFavourite = true,
            onOnlyFavouriteChange = {},
            modifier = Modifier.padding(Dimensions.Padding.screen),
        )
    }
}
