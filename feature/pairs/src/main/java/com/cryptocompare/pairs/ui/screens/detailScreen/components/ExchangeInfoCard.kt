package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary
import kotlin.math.abs

@Composable
fun ExchangeInfoCard(
    exchange: ProviderDetail,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = Dimensions.Border.card,
                    color = MaterialTheme.colorScheme.borderPrimary,
                    shape = MaterialTheme.shapes.large,
                ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.cardLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exchange.provider.name ?: "Unknown Exchange",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                StatusBadge(status = exchange.provider.status)
            }

            exchange.provider.webSite?.takeIf { it.isNotBlank() }?.let { website ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textTertiary,
                        modifier = Modifier.size(PairsConstants.DetailScreen.websiteIconSize),
                    )
                    Text(
                        text = website,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textTertiary,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.borderPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
            ) {
                // бэкенд: priceSell = ask (по ней пользователь покупает),
                // priceBuy = bid (по ней пользователь продаёт)
                exchange.priceSell?.let { price ->
                    PriceBlock(
                        label = stringResource(R.string.pair_detail_buy_price),
                        price = price,
                        modifier = Modifier.weight(1f),
                    )
                }
                exchange.priceBuy?.let { price ->
                    PriceBlock(
                        label = stringResource(R.string.pair_detail_sell_price),
                        price = price,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (exchange.priceBuy != null && exchange.priceSell != null) {
                val spread = abs(exchange.priceSell!! - exchange.priceBuy!!)
                val spreadPct = if (exchange.priceSell!! > 0) spread / exchange.priceSell!! * 100 else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.pair_detail_spread),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                    Text(
                        text = PairsConstants.DetailScreen.SPREAD_FORMAT.format(spreadPct),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }
        }
    }
}
