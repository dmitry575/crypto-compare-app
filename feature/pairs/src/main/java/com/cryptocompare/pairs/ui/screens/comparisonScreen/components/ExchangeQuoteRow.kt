package com.cryptocompare.pairs.ui.screens.comparisonScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * Строка с [isStale] приглушается целиком. Без этого таблица выглядит сломанной:
 * у пары бывает биржа с самой низкой ценой покупки **без** отметки о выгоде —
 * потому что цена стоит третий час, и бэкенд её отбросил. Приглушённая строка
 * объясняет это без слов; словами не выходит — на подпись рядом с объёмом
 * в колонке имени места нет.
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
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = quote.provider.name ?: stringResource(R.string.pair_detail_unknown_exchange),
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (isStale) {
                        MaterialTheme.colorScheme.textTertiary
                    } else {
                        MaterialTheme.colorScheme.textPrimary
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.pair_comparison_volume,
                        quote.quoteVolume24h?.toCompactVolumeString()
                            ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                    ),
                style = NumericType.Caption,
                color = MaterialTheme.colorScheme.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        PriceCell(
            price = quote.priceSell,
            highlighted = isBestAsk,
            dimmed = isStale,
            contentDescription = stringResource(R.string.pair_comparison_best_buy),
        )
        PriceCell(
            price = quote.priceBuy,
            highlighted = isBestBid,
            dimmed = isStale,
            muted = true,
            contentDescription = stringResource(R.string.pair_comparison_best_sell),
        )
    }
}

/**
 * Ячейка цены.
 *
 * Колонка фиксированной ширины, а плашка обнимает само число: если красить фоном
 * всю колонку, подсветка уезжает влево от цены и стыкуется с соседней.
 *
 * Прочерк вместо нуля: биржа может не отдавать одну из сторон, и ноль читался бы
 * как «отдают даром».
 */
@Composable
private fun PriceCell(
    price: Double?,
    highlighted: Boolean,
    dimmed: Boolean,
    contentDescription: String,
    muted: Boolean = false,
) {
    val text = price?.toCompactPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER
    val color =
        when {
            highlighted -> MaterialTheme.colorScheme.primary
            price == null || dimmed -> MaterialTheme.colorScheme.textTertiary
            muted -> MaterialTheme.colorScheme.textSecondary
            else -> MaterialTheme.colorScheme.textPrimary
        }

    Box(
        modifier = Modifier.width(PairsConstants.ComparisonScreen.priceColumnWidth),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = NumericType.Caption,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier =
                Modifier
                    .then(
                        if (highlighted) {
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.accentSoft,
                                    shape = RoundedCornerShape(Dimensions.Radius.sm),
                                ).semantics { this.contentDescription = contentDescription }
                        } else {
                            Modifier
                        },
                    ).padding(
                        horizontal = Dimensions.Spacing.xs,
                        vertical = Dimensions.Spacing.xxs,
                    ),
        )
    }
}
