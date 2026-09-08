package com.cryptocompare.pairs.ui.screens.comparisonScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.toCompactPriceString
import com.cryptocompare.helpers.toCompactVolumeString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Одна биржа в таблице сравнения: имя, цена покупки, цена продажи.
 *
 * Выигравшая ячейка подсвечивается акцентом, а не зелёным: зелёный и красный
 * заняты направлением цены. Кто выиграл, решает **бэкенд** — [isBestAsk] и
 * [isBestBid] приходят из его best-выдачи. Считать по этому же списку нельзя:
 * он приходит без фильтра свежести, и застрявшая цена получала бы отметку
 * «здесь выгоднее» именно потому, что застряла в выгодную сторону.
 *
 * Под именем — объём: он показывает, стоит ли верить заманчивой цене. Рядом с
 * ним отметка о несвежести, если биржа давно не присылала котировку.
 */
@Composable
internal fun ExchangeQuoteRow(
    quote: ProviderDetail,
    isBestAsk: Boolean,
    isBestBid: Boolean,
    isStale: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Height.listItemSmall)
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = quote.provider.name ?: stringResource(R.string.pair_detail_unknown_exchange),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metaLine(quote, isStale),
                style = NumericType.Caption,
                color = MaterialTheme.colorScheme.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        PriceCell(
            price = quote.priceSell,
            highlighted = isBestAsk,
            contentDescription = stringResource(R.string.pair_comparison_best_buy),
        )
        PriceCell(
            price = quote.priceBuy,
            highlighted = isBestBid,
            contentDescription = stringResource(R.string.pair_comparison_best_sell),
            muted = true,
        )
    }
}

@Composable
private fun metaLine(
    quote: ProviderDetail,
    isStale: Boolean,
): String {
    // без подписи «объём»: вместе с отметкой о несвежести строка не влезает
    // в колонку имени — на 360dp ей достаётся ~136dp, а подпись съедает ~50dp
    val volume =
        quote.quoteVolume24h?.toCompactVolumeString()
            ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER

    return if (isStale) {
        volume +
            PairsConstants.ComparisonScreen.META_SEPARATOR +
            stringResource(R.string.pair_comparison_stale)
    } else {
        volume
    }
}

/**
 * Ячейка цены. Прочерк вместо нуля: биржа может не отдавать одну из сторон, и
 * ноль читался бы как «отдают даром».
 */
@Composable
private fun PriceCell(
    price: Double?,
    highlighted: Boolean,
    contentDescription: String,
    muted: Boolean = false,
) {
    val text = price?.toCompactPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER
    val color =
        when {
            price == null -> MaterialTheme.colorScheme.textTertiary
            highlighted -> MaterialTheme.colorScheme.primary
            muted -> MaterialTheme.colorScheme.textSecondary
            else -> MaterialTheme.colorScheme.textPrimary
        }

    Text(
        text = text,
        style = NumericType.Caption,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
        softWrap = false,
        modifier =
            Modifier
                .width(PairsConstants.ComparisonScreen.priceColumnWidth)
                .then(
                    if (highlighted) {
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.accentSoft,
                                shape = RoundedCornerShape(Dimensions.Radius.sm),
                            ).padding(
                                horizontal = Dimensions.Spacing.xxs,
                                vertical = Dimensions.Spacing.xxs,
                            ).semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier.padding(
                            horizontal = Dimensions.Spacing.xxs,
                            vertical = Dimensions.Spacing.xxs,
                        )
                    },
                ),
    )
}
