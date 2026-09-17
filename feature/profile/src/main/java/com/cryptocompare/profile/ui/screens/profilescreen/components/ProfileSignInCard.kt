package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cryptocompare.profile.R
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary

/**
 * Приглашение войти вместо шапки с почтой — для гостя.
 *
 * Каталог, детали и сравнение работают без аккаунта, поэтому вход предлагается
 * там, где он даёт что-то конкретное: избранное, общее на всех устройствах.
 */
@Composable
internal fun ProfileSignInCard(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileGroup(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.cardMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        ) {
            Text(
                text = stringResource(R.string.profile_guest_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.profile_guest_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textSecondary,
            )

            AppPrimaryButton(
                text = stringResource(R.string.profile_sign_in),
                onClick = onSignInClick,
            )
        }
    }
}
