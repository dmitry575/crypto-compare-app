package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.profile.R
import com.cryptocompare.ui.locale.ProvideWindowLanguage
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Выбор биржи, с которой открывается пара.
 *
 * Шторка со списком, а не ряд чипов: бирж больше двадцати. Первым пунктом —
 * «Первая доступная»: у половины пар выбранной площадки просто нет, и это же
 * поведение было до настройки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DefaultExchangeSheet(
    providers: List<Provider>,
    selectedProviderId: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // читается снаружи окна шторки: внутри LocalContext уже без подмены языка
    val outerContext = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.bgCard,
        modifier = modifier,
    ) {
        ProvideWindowLanguage(outerContext) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.profile_default_exchange),
                    style = OverlineType,
                    color = MaterialTheme.colorScheme.textTertiary,
                    modifier =
                        Modifier.padding(
                            horizontal = Dimensions.Padding.screenHorizontal,
                            vertical = Dimensions.Spacing.xs,
                        ),
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ExchangeRow(
                            name = stringResource(R.string.profile_default_exchange_any),
                            selected = selectedProviderId == null,
                            onClick = { onSelect(null) },
                        )
                    }

                    items(providers, key = { it.id }) { provider ->
                        ExchangeRow(
                            name = provider.name.orEmpty().ifBlank { provider.id.toString() },
                            selected = provider.id == selectedProviderId,
                            onClick = { onSelect(provider.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Height.listItemSmall)
                .clickable(onClick = onClick)
                .padding(horizontal = Dimensions.Padding.screenHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.textPrimary
                },
        )

        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimensions.IconSize.md),
            )
        }
    }
}
