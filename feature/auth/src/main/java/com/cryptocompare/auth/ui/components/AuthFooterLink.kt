package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AuthFooterLink(
    actionText: String,
    onClick: () -> Unit,
    prompt: String = "",
) {
    if (prompt.isNotEmpty()) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }

    Text(
        text = actionText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(onClick = onClick),
    )
}
