package com.cryptocompare.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.cryptocompare.ui.theme.inputBackground
import com.cryptocompare.ui.theme.inputBorder
import com.cryptocompare.ui.theme.inputBorderFocused
import com.cryptocompare.ui.theme.textTertiary

/**
 * Поле ввода форм авторизации и профиля: единый вид, показ/скрытие пароля из коробки.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val visualTransformation =
        if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.textTertiary,
            )
        },
        leadingIcon = {
            Text(
                text = leadingIcon,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        trailingIcon =
            if (isPassword) {
                {
                    val icon: ImageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textTertiary,
                        )
                    }
                }
            } else {
                null
            },
        isError = isError,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle =
            TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            ),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.inputBackground,
                unfocusedContainerColor = MaterialTheme.colorScheme.inputBackground,
                focusedBorderColor = MaterialTheme.colorScheme.inputBorderFocused,
                unfocusedBorderColor = MaterialTheme.colorScheme.inputBorder,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.inputBorderFocused,
                focusedLeadingIconColor = MaterialTheme.colorScheme.inputBorderFocused,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.textTertiary,
            ),
    )
}
