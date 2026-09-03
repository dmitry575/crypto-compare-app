package com.cryptocompare.pairs.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.cryptocompare.helpers.priceChangeSign
import com.cryptocompare.helpers.toSignedPercentString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.priceChangeColor
import com.cryptocompare.ui.theme.textTertiary

/**
 * Изменение цены за 24 часа. Общий для каталога и экрана деталей: одна и та же
 * величина не должна выглядеть на двух экранах по-разному.
 *
 * `null` рисуется прочерком, а не пропускается: биржа может не отдавать поле,
 * и исчезающая строка дёргала бы раскладку от пары к паре.
 */
@Composable
internal fun Change24hLabel(
    change24h: Double?,
    modifier: Modifier = Modifier,
    style: TextStyle = NumericType.Caption,
) {
    Text(
        text = change24h?.toSignedPercentString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
        style = style,
        color =
            if (change24h == null) {
                MaterialTheme.colorScheme.textTertiary
            } else {
                MaterialTheme.colorScheme.priceChangeColor(change24h.priceChangeSign())
            },
        maxLines = 1,
        modifier = modifier,
    )
}
