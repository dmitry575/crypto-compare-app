package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.cryptoError

@Composable
fun AuthErrorMessage(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.cryptoError,
        style = MaterialTheme.typography.bodySmall,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.cryptoError.copy(alpha = 0.1f))
                .padding(Dimensions.Padding.cardSmall),
    )
}
