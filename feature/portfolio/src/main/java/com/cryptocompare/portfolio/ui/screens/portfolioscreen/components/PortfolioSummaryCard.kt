package com.cryptocompare.portfolio.ui.screens.portfolioscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.priceChangeSign
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.helpers.toSignedPercentString
import com.cryptocompare.helpers.toSignedPriceString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.portfolio.PortfolioSummary
import com.cryptocompare.portfolio.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.divider
import com.cryptocompare.ui.theme.priceChangeColor
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Итог портфеля: сколько он стоит сейчас, сколько в него вложено и что вышло.
 *
 * Карточка стоит на месте и при [summary] = `null` — так бывает, пока каталог
 * не подъехал и цен нет ни по одной позиции. Прочерк честнее пустоты: список
 * позиций под ним уже виден, и исчезнувший итог читался бы как «ничего не
 * нажито».
 *
 * Зелёный и красный здесь по делу: прибыль — это и есть направление цены,
 * ровно то, подо что эти цвета заняты (правило 2 дизайн-системы).
 */
@Composable
internal fun PortfolioSummaryCard(
    summary: PortfolioSummary?,
    modifier: Modifier = Modifier,
) {
    val profitSign = summary?.profitPercent?.priceChangeSign() ?: 0

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
                ).padding(Dimensions.Padding.cardLarge)
                // TalkBack читает итог одним куском, а не пятью отдельными
                // остановками: «стоимость сейчас», число, прибыль, «вложено», число
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        Text(
            text = stringResource(R.string.portfolio_total_value),
            style = OverlineType,
            color = MaterialTheme.colorScheme.textTertiary,
        )

        Text(
            text = summary?.currentValue?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
            style = NumericType.Hero,
            color = MaterialTheme.colorScheme.textPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary?.profit?.toSignedPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Medium,
                color = MaterialTheme.colorScheme.priceChangeColor(profitSign),
                maxLines = 1,
            )
            summary?.profitPercent?.let { percent ->
                Text(
                    text = stringResource(R.string.portfolio_profit_percent, percent.toSignedPercentString()),
                    style = NumericType.Small,
                    color = MaterialTheme.colorScheme.priceChangeColor(profitSign),
                    maxLines = 1,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.divider)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.portfolio_invested),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = summary?.invested?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Small,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
            )
        }

        if (summary != null && summary.positionsWithoutPrice > 0) {
            Text(
                text = stringResource(R.string.portfolio_missing_prices),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
        }
    }
}
