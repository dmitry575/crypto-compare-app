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
import androidx.compose.ui.text.font.FontWeight
import com.cryptocompare.helpers.bidAskSpreadPercent
import com.cryptocompare.helpers.toCompactVolumeString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.components.Change24hLabel
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

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
                    text = exchange.provider.name ?: stringResource(R.string.pair_detail_unknown_exchange),
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

            val ask = exchange.priceSell
            val bid = exchange.priceBuy
            if (ask != null && bid != null) {
                DetailStatRow(label = stringResource(R.string.pair_detail_spread)) {
                    Text(
                        text =
                            PairsConstants.DetailScreen.SPREAD_FORMAT.format(
                                bidAskSpreadPercent(ask = ask, bid = bid),
                            ),
                        style = NumericType.Caption,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }

            // 24ч-статистика этой биржи, а не сводная по рынку: на разных биржах
            // одна и та же пара живёт по-разному, и усреднение это скрывает
            DetailStatRow(label = stringResource(R.string.pair_detail_change_24h)) {
                Change24hLabel(change24h = exchange.change24h)
            }
            DetailStatRow(label = stringResource(R.string.pair_detail_volume_24h_quote)) {
                VolumeValue(volume = exchange.quoteVolume24h)
            }
            DetailStatRow(label = stringResource(R.string.pair_detail_volume_24h_base)) {
                VolumeValue(volume = exchange.volume24h)
            }
        }
    }
}

/** Объём одной строкой: биржа может его не отдавать, тогда на месте числа прочерк. */
@Composable
private fun VolumeValue(volume: Double?) {
    Text(
        text = volume?.toCompactVolumeString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
        style = NumericType.Caption,
        color =
            if (volume == null) {
                MaterialTheme.colorScheme.textTertiary
            } else {
                MaterialTheme.colorScheme.textSecondary
            },
        maxLines = 1,
    )
}
