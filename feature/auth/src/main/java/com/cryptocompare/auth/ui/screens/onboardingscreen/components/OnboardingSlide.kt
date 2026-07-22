package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textSecondary

@Composable
internal fun OnboardingSlide(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.Padding.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
