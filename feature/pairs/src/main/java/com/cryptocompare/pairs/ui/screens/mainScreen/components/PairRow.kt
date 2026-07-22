package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.cryptocompare.helpers.parseTicker
import com.cryptocompare.helpers.toCompactPriceString
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Строка каталога.
 *
 * Колонок «Max/Min» больше нет: две цены одинакового веса заставляли вычитать
 * одно из другого в уме, хотя разброс — единственное, ради чего сюда заходят.
 * Теперь видно лучшую цену и готовый разброс, а число бирж под тикером
 * подсказывает, насколько этому разбросу можно верить.
 */
@Composable
fun PairRow(
    pair: PairUiItem,
    modifier: Modifier = Modifier,
    rowHeight: Dp? = null,
    isFavourite: Boolean = false,
    onFavouriteClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val parts = remember(pair.ticker) { pair.ticker.parseTicker() }
    val base = parts?.first ?: pair.ticker
    val rowModifier = if (rowHeight != null) modifier.height(rowHeight) else modifier

    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PairBadge(base = base)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = tickerLabel(pair.ticker, parts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.pair_detail_exchange_count,
                        pair.exchangeCount,
                        pair.exchangeCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
                maxLines = 1,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = pair.maxPrice.toCompactPriceString(),
                style = NumericType.Small,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
            )
            SpreadBadge(spreadPercent = pair.spreadPercent)
        }

        IconButton(
            onClick = onFavouriteClick,
            modifier = Modifier.size(Dimensions.TouchTarget.min),
        ) {
            Icon(
                imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription =
                    stringResource(
                        if (isFavourite) {
                            R.string.pairs_remove_from_favorites
                        } else {
                            R.string.pairs_add_to_favorites
                        },
                    ),
                tint =
                    if (isFavourite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.textTertiary
                    },
            )
        }
    }
}

/** Котировка приглушена: одинаковый USDT в каждой строке не должен спорить с базой. */
@Composable
private fun tickerLabel(
    ticker: String,
    parts: Pair<String, String>?,
): AnnotatedString {
    val quoteColor = MaterialTheme.colorScheme.textTertiary
    return remember(ticker, parts, quoteColor) {
        if (parts == null) {
            AnnotatedString(ticker)
        } else {
            buildAnnotatedString {
                append(parts.first)
                withStyle(SpanStyle(color = quoteColor)) {
                    append("/")
                    append(parts.second)
                }
            }
        }
    }
}
