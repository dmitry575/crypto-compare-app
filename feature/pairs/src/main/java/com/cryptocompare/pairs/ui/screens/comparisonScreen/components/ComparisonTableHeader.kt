package com.cryptocompare.pairs.ui.screens.comparisonScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.textTertiary

/**
 * Подписи колонок таблицы — один раз сверху, а не в каждой строке.
 *
 * Ширины и отступы повторяют [ExchangeQuoteRow] дословно: у колонок цен
 * фиксированная ширина и одинаковый внутренний отступ, иначе заголовок и числа
 * встанут по разным правым краям.
 */
@Composable
internal fun ComparisonTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Spacing.xs,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        HeaderCell(text = stringResource(R.string.pair_comparison_buy))
        HeaderCell(text = stringResource(R.string.pair_comparison_sell))
    }
}

@Composable
private fun HeaderCell(text: String) {
    Text(
        text = text,
        style = NumericType.Caption,
        color = MaterialTheme.colorScheme.textTertiary,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier =
            Modifier
                .width(PairsConstants.ComparisonScreen.priceColumnWidth)
                .padding(horizontal = Dimensions.Spacing.xs),
    )
}
