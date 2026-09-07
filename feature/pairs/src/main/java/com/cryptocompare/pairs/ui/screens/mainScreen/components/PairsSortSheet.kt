package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.model.symbol.CatalogSort
import com.cryptocompare.model.symbol.CatalogSorting
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Выбор сортировки каталога.
 *
 * Шторка, а не ещё один ряд чипов: полей пять, у каждого два направления —
 * десять состояний в ленту не поместятся, а прятать половину за прокрутку
 * значит прятать половину функции.
 *
 * Повторный выбор того же поля переворачивает порядок, поэтому стрелка стоит
 * только у выбранного: она показывает, что будет, а не предлагает второй выбор.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairsSortSheet(
    sorting: CatalogSorting,
    onSortSelected: (CatalogSort) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.bgCard,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimensions.Spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.pairs_sort_title),
                style = OverlineType,
                color = MaterialTheme.colorScheme.textTertiary,
                modifier =
                    Modifier.padding(
                        horizontal = Dimensions.Padding.screenHorizontal,
                        vertical = Dimensions.Spacing.xs,
                    ),
            )

            CatalogSort.entries.forEach { field ->
                val selected = field == sorting.field

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimensions.Height.listItemSmall)
                            .clickable { onSortSelected(field) }
                            .padding(horizontal = Dimensions.Padding.screenHorizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(field.labelRes()),
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
                            imageVector =
                                if (sorting.ascending) {
                                    Icons.Filled.ArrowUpward
                                } else {
                                    Icons.Filled.ArrowDownward
                                },
                            contentDescription =
                                stringResource(
                                    if (sorting.ascending) {
                                        R.string.pairs_sort_ascending
                                    } else {
                                        R.string.pairs_sort_descending
                                    },
                                ),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimensions.IconSize.md),
                        )
                    }
                }
            }
        }
    }
}

internal fun CatalogSort.labelRes(): Int =
    when (this) {
        CatalogSort.NAME -> R.string.pairs_sort_name
        CatalogSort.PRICE -> R.string.pairs_sort_price
        CatalogSort.CHANGE -> R.string.pairs_sort_change
        CatalogSort.SPREAD -> R.string.pairs_sort_spread
        CatalogSort.VOLUME -> R.string.pairs_sort_volume
    }
