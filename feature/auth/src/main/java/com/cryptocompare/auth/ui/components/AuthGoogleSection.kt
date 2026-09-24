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
import com.cryptocompare.auth.util.AuthConstants
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
 * случае.
 *
 * Сервисы есть, но устарели или выключены — кнопка остаётся: по нажатию
 * система сама предложит их обновить или включить, и следующее нажатие уже
 * войдёт. Статус проверяется в момент нажатия, а не при показе: после
 * обновления экран не пересоздаётся.
 */
@Composable
internal fun AuthGoogleSection(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val availability = GoogleApiAvailability.getInstance()
    val playServicesUsable =
        remember(context) {
            val status = availability.isGooglePlayServicesAvailable(context)
            status == ConnectionResult.SUCCESS || status in AuthConstants.PLAY_SERVICES_FIXABLE_STATUSES
        }
    if (!playServicesUsable) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

        AuthDivider(text = stringResource(R.string.auth_divider_or))

        Spacer(modifier = Modifier.height(Dimensions.Spacing.md))

        AuthGoogleButton(
            text = stringResource(R.string.auth_continue_google),
            onClick = {
                if (availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
                    onClick()
                } else {
                    context.findActivity()?.let(availability::makeGooglePlayServicesAvailable)
                }
            },
        )
    }
}
