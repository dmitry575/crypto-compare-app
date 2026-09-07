package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.components.AppChipRow
import com.cryptocompare.ui.components.AppFilterChip
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.statusInactive

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
    AppChipRow(modifier = modifier) {
        exchanges.forEachIndexed { index, exchange ->
            val isActive = exchange.provider.status == ProviderStatus.Enabled

            AppFilterChip(
                label =
                    exchange.provider.name
                        ?: stringResource(R.string.pair_detail_unknown_exchange),
                selected = index == selectedIndex,
                onClick = { onExchangeSelected(index) },
                leading = {
                    Box(
                        modifier =
                            Modifier
                                .size(Dimensions.IconSize.chipDot)
                                .background(
                                    color =
                                        if (isActive) {
                                            MaterialTheme.colorScheme.statusActive
                                        } else {
                                            MaterialTheme.colorScheme.statusInactive
                                        },
                                    shape = CircleShape,
                                ),
                    )
                },
            )
        }
    }
}
