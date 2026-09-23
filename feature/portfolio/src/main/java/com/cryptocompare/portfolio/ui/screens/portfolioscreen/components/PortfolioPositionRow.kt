package com.cryptocompare.portfolio.ui.screens.portfolioscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.priceChangeSign
import com.cryptocompare.helpers.toCompactPriceString
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.helpers.toSignedPercentString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.portfolio.PortfolioHolding
import com.cryptocompare.portfolio.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.priceChangeColor
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Позиция в списке: слева — что и почём куплено, справа — сколько стоит сейчас
 * и что на этом вышло.
 *
 * Под количеством — биржа, чей bid взят ценой: он переезжает между
 * площадками, и стоимость без этой подписи прыгала бы необъяснимо.
 *
 * Цены может не быть: символ выпал из каталога или каталог ещё не подъехал.
 * Тогда справа прочерк, а не ноль — «стоит ноль» это другое утверждение.
 */
@Composable
internal fun PortfolioPositionRow(
    holding: PortfolioHolding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profitSign = holding.profitPercent?.priceChangeSign() ?: 0

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Height.listItemStats)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Dimensions.Padding.cardMedium,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
        ) {
            Text(
                text = holding.position.ticker.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.portfolio_position_cost,
                        holding.position.amount.toCompactPriceString(),
                        holding.position.buyPrice.toCompactPriceString(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // лучший bid переезжает между биржами, и без этой строки было
            // непонятно, почему позиция вдруг подешевела
            holding.priceExchange?.let { exchange ->
                Text(
                    text = stringResource(R.string.portfolio_price_source, exchange),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
        ) {
            Text(
                text = holding.currentValue?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Small,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = holding.profitPercent?.toSignedPercentString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Caption,
                color = MaterialTheme.colorScheme.priceChangeColor(profitSign),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
