package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgSunk
import com.cryptocompare.ui.theme.inputBorderFocused
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Поиск по каталогу.
 *
 * Плавающей подписи «Search:» больше нет: она занимала место, дублировала иконку
 * и в поднятом состоянии обрезала верх поля. Подсказка живёт внутри и исчезает
 * при вводе, а крестик даёт вернуться ко всему каталогу без стирания по букве.
 */
@Composable
internal fun PairsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(Dimensions.Radius.md),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = stringResource(R.string.pairs_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.textTertiary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.IconSize.md),
                tint = MaterialTheme.colorScheme.textTertiary,
            )
        },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.pairs_clear_search),
                            tint = MaterialTheme.colorScheme.textTertiary,
                        )
                    }
                }
            } else {
                null
            },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.textPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.textPrimary,
                focusedContainerColor = MaterialTheme.colorScheme.bgSunk,
                unfocusedContainerColor = MaterialTheme.colorScheme.bgSunk,
                focusedBorderColor = MaterialTheme.colorScheme.inputBorderFocused,
                unfocusedBorderColor = MaterialTheme.colorScheme.bgSunk,
                cursorColor = MaterialTheme.colorScheme.inputBorderFocused,
            ),
    )
}
