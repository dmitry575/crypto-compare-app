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
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.portfolio.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.textSecondary

/** Позиция в списке: что, сколько и почём покупалось. Прибыль добавится следующим шагом. */
@Composable
internal fun PortfolioPositionRow(
    position: PortfolioPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Height.listItemSmall)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Dimensions.Padding.cardMedium,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs)) {
            Text(
                text = position.ticker.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.portfolio_average_price, position.buyPrice.toPriceString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary,
            )
        }

        Text(
            text = position.amount.toPriceString(),
            style = NumericType.Small,
        )
    }
}
