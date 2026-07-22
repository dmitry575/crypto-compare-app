package com.cryptocompare.auth.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "logo")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "logoOffset",
    )

    Text(
        text = "🚀",
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.graphicsLayer(translationY = offset),
    )
}
