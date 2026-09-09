package com.cryptocompare.pairs.ui.screens.comparisonScreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Пояснение вместо данных: пара на одной бирже, пустой список, ошибка.
 *
 * Один компонент на все три случая — они отличаются только текстом, а заводить
 * под каждый свой экран значит трижды повторить одну и ту же разметку.
 */
@Composable
internal fun ComparisonMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.Padding.screenHorizontal,
                    vertical = Dimensions.Spacing.lg,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
