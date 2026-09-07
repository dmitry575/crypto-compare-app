package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.components.AppChipRow
import com.cryptocompare.ui.components.AppFilterChip
import com.cryptocompare.ui.theme.CryptoCompareThemePreview
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.ThemePreviews

/**
 * Быстрые фильтры каталога.
 *
 * Лента чипов, а не сегментированный контрол: тот делит ширину поровну между
 * вариантами, поэтому «Все» получало столько же места, сколько «Избранное», а
 * на четырёх вариантах подписи начинали обрезаться уже при обычном шрифте.
 * Чипы меряются по содержимому, лишнее уезжает за край прокруткой.
 *
 * Сюда же встанут «Рост» и «Падение», чип выбора бирж со счётчиком и чип
 * сортировки с направлением — ряд от этого не удвоится.
 */
@Composable
internal fun PairsFilterRow(
    onlyFavourite: Boolean,
    onOnlyFavouriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppChipRow(modifier = modifier) {
        AppFilterChip(
            label = stringResource(R.string.pairs_filter_all),
            selected = !onlyFavourite,
            onClick = { onOnlyFavouriteChange(false) },
        )
        AppFilterChip(
            label = stringResource(R.string.pairs_filter_favorites),
            selected = onlyFavourite,
            onClick = { onOnlyFavouriteChange(true) },
        )
    }
}

@ThemePreviews
@Composable
private fun PairsFilterRowPreview() {
    CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme()) {
        PairsFilterRow(
            onlyFavourite = false,
            onOnlyFavouriteChange = {},
            modifier = Modifier.padding(Dimensions.Padding.screen),
        )
    }
}
