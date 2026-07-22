package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.statusInactive
import com.cryptocompare.ui.theme.textSecondary

/**
 * Выбор биржи.
 *
 * Выбранный чип красился в secondary, то есть в фиолетовый, а стоящий ниже ряд
 * масштабов графика — в primary. Два одинаковых ряда чипов на одном экране
 * различались только цветом, и различие ничего не означало. Теперь акцент один,
 * а биржи отличает точка статуса: работает она или выключена.
 */
@Composable
fun ExchangeSelector(
    exchanges: List<ProviderDetail>,
    selectedIndex: Int,
    onExchangeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        exchanges.forEachIndexed { index, exchange ->
            val isSelected = index == selectedIndex
            val shape = RoundedCornerShape(Dimensions.Radius.full)

            Row(
                modifier =
                    Modifier
                        .background(
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                            shape = shape,
                        ).border(
                            width = Dimensions.Border.thin,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.borderPrimary
                                },
                            shape = shape,
                        ).clickable { onExchangeSelected(index) }
                        .padding(
                            horizontal = Dimensions.Padding.chipHorizontal,
                            vertical = Dimensions.Padding.chipVertical,
                        ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .size(Dimensions.IconSize.chipDot)
                            .background(
                                color =
                                    if (exchange.provider.status == ProviderStatus.Enabled) {
                                        MaterialTheme.colorScheme.statusActive
                                    } else {
                                        MaterialTheme.colorScheme.statusInactive
                                    },
                                shape = CircleShape,
                            ),
                    content = {},
                )

                Text(
                    text =
                        exchange.provider.name
                            ?: stringResource(R.string.pair_detail_unknown_exchange),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.textSecondary
                        },
                )
            }
        }
    }
}
