package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Экран целиком вместо списка: показывается, когда первая загрузка провалилась
 * и показать нечего. Если старые данные уже есть, ошибка идёт снекбаром.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Dimensions.Padding.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.lg, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textTertiary,
            modifier = Modifier.size(Dimensions.IconSize.xxl),
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
        )

        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.pairs_error_retry))
        }
    }
}
