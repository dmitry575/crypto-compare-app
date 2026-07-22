package com.cryptocompare.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgSunk
import com.cryptocompare.ui.theme.textDisabled

/**
 * Главная кнопка экрана. Заливка сплошная: градиент был единственным местом,
 * где встречался второй цвет палитры, и ради него держался целый набор токенов.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .height(Dimensions.Height.button),
        shape = RoundedCornerShape(Dimensions.Radius.button),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.bgSunk,
                disabledContentColor = MaterialTheme.colorScheme.textDisabled,
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
