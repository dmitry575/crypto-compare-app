package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary

/**
 * Пустой результат. Раньше на его месте оставался голый экран с заголовками
 * колонок, и было не понять — то ли ничего не нашлось, то ли всё сломалось.
 */
@Composable
internal fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Dimensions.Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
