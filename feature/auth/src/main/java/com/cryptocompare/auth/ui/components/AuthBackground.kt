package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cryptocompare.ui.theme.bgPrimary

/**
 * Фон экранов авторизации. Заливка идёт под системные панели, а содержимое
 * от них отступает: приложение рисуется edge-to-edge, и без этого логотип
 * заезжал под строку состояния.
 */
@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.bgPrimary),
    ) {
        Box(
            modifier = Modifier.systemBarsPadding(),
            content = content,
        )
    }
}
