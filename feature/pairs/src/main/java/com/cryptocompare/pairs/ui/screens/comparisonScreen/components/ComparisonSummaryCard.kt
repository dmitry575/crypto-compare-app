package com.cryptocompare.pairs.ui.screens.comparisonScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.isNotableSpread
import com.cryptocompare.helpers.toPercentString
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.comparison.PairComparison
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Ответ экрана: где купить, где продать и сколько на этом выйдет.
 *
 * Числа берутся из best-выдачи бэкенда, а не из таблицы ниже: та приходит без
 * фильтра свежести, и биржа с зависшей ценой выглядела бы в ней самой выгодной.
 *
 * Цвет только у процента и только когда он в плюсе. Зелёный и красный в
 * приложении заняты направлением цены, а возможность заработать на разнице —
 * это не «подорожало»; в норме же спред отрицателен, и красить каждую пару
 * значило бы не выделять ничего.
 */
@Composable
internal fun ComparisonSummaryCard(
    comparison: PairComparison,
    modifier: Modifier = Modifier,
) {
    val spread = comparison.spreadPercent
    val notable = spread != null && spread.isNotableSpread()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.bgCard,
                    shape = RoundedCornerShape(Dimensions.Radius.card),
                ).border(
                    width = Dimensions.Border.card,
                    color = MaterialTheme.colorScheme.borderPrimary,
                    shape = RoundedCornerShape(Dimensions.Radius.card),
                ).padding(Dimensions.Padding.cardLarge),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
    ) {
        Text(
            text = stringResource(R.string.pair_detail_spread_title),
            style = OverlineType,
            color = MaterialTheme.colorScheme.textTertiary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        ) {
            SummarySide(
                label = stringResource(R.string.pair_comparison_buy_label),
                exchange = comparison.exchangeName(comparison.bestAskProviderId),
                price = comparison.bestAskPrice,
                alignment = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            SummarySide(
                label = stringResource(R.string.pair_comparison_sell_label),
                exchange = comparison.exchangeName(comparison.bestBidProviderId),
                price = comparison.bestBidPrice,
                alignment = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Crypto.spreadTrack)
                    .background(
                        color =
                            if (notable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.accentSoft
                            },
                        shape = RoundedCornerShape(Dimensions.Radius.full),
                    ),
            content = {},
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summaryCaption(comparison, notable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = spread?.toPercentString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Medium,
                color =
                    if (notable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.textPrimary
                    },
            )
        }
    }
}

/**
 * Слева от процента — что он значит.
 *
 * Разница в котируемом активе полезна, только когда на ней можно заработать;
 * в обычном случае честнее сказать словами, что продажа дешевле покупки, чем
 * показывать минус вторым числом подряд.
 */
@Composable
private fun summaryCaption(
    comparison: PairComparison,
    notable: Boolean,
): String {
    val difference = comparison.difference

    return when {
        comparison.spreadPercent == null -> stringResource(R.string.pair_comparison_no_best)
        notable && difference != null ->
            stringResource(R.string.pair_detail_spread_difference, difference.toPriceString())
        else -> stringResource(R.string.pair_comparison_no_profit)
    }
}

@Composable
private fun SummarySide(
    label: String,
    exchange: String,
    price: Double?,
    alignment: TextAlign,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        horizontalAlignment = if (alignment == TextAlign.End) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textTertiary,
            textAlign = alignment,
            maxLines = 1,
        )
        Text(
            text = exchange,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textPrimary,
            textAlign = alignment,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = price?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
            style = NumericType.Medium,
            color = MaterialTheme.colorScheme.textPrimary,
            textAlign = alignment,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PairComparison.exchangeName(providerId: Int?): String =
    quotes
        .firstOrNull { it.provider.id == providerId }
        ?.provider
        ?.name
        ?: stringResource(R.string.pair_detail_unknown_exchange)
