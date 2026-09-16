package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cryptocompare.profile.R
import com.cryptocompare.ui.locale.ProvideWindowLanguage

@Composable
internal fun ProfileConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
) {
    // Заголовок и кнопку подтверждения экран резолвит до открытия диалога, а эта
    // строка читается уже внутри его окна — там язык приложения теряется.
    val outerContext = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            ProvideWindowLanguage(outerContext) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.profile_dialog_cancel))
                }
            }
        },
    )
}
