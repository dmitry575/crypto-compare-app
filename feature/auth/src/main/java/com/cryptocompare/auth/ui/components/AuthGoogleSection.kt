package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cryptocompare.auth.R
import com.cryptocompare.ui.theme.Dimensions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * «или» и кнопка входа через Google — либо ничего, если на устройстве нет
 * сервисов Google Play.
 *
 * Без них `GoogleSignIn` не открывает выбор аккаунта, а падает при запуске
 * интента: так ведут себя телефоны Huawei с HMS и образы AOSP. Кнопка, которая
 * заведомо не сработает, хуже отсутствующей — вход по почте остаётся в любом
 * случае. Показываем только при `SUCCESS`: устаревшие сервисы — редкость, и
 * почта там тоже работает.
 */
@Composable
internal fun AuthGoogleSection(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playServicesAvailable =
        remember(context) {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }
    if (!playServicesAvailable) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

        AuthDivider(text = stringResource(R.string.auth_divider_or))

        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

        AuthGoogleButton(
            text = stringResource(R.string.auth_continue_google),
            onClick = onClick,
        )
    }
}
