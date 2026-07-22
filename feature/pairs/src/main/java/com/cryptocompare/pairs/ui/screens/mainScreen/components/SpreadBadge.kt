package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cryptocompare.helpers.isNotableSpread
import com.cryptocompare.helpers.toPercentString
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.bgSunk
import com.cryptocompare.ui.theme.textTertiary

/**
 * Насколько цена расходится между биржами.
 *
 * Подсветка акцентом, а не зелёным: зелёный и красный в приложении означают
 * только направление цены, и большой разброс — это не «подорожало». Ниже порога
 * разброс тонет в комиссиях, поэтому плашка уходит в приглушённый вид.
 */
@Composable
internal fun SpreadBadge(
    spreadPercent: Double,
    modifier: Modifier = Modifier,
) {
    val notable = spreadPercent.isNotableSpread()

    Text(
        text = spreadPercent.toPercentString(),
        style = NumericType.Caption,
        color =
            if (notable) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.textTertiary
            },
        modifier =
            modifier
                .background(
                    color =
                        if (notable) {
                            MaterialTheme.colorScheme.accentSoft
                        } else {
                            MaterialTheme.colorScheme.bgSunk
                        },
                    shape = RoundedCornerShape(Dimensions.Radius.full),
                ).padding(
                    horizontal = Dimensions.Spacing.xs,
                    vertical = Dimensions.Spacing.xxs,
                ),
    )
}
