package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.cryptoError
import com.cryptocompare.ui.theme.cryptoSuccess

/**
 * Полоса разброса в миниатюре — тот же элемент, что встретит пользователя
 * на детальном экране. Онбординг заранее учит его читать.
 */
@Composable
internal fun SpreadTrackArt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = AuthConstants.Onboarding.SAMPLE_LOW,
                style = NumericType.Medium,
                color = MaterialTheme.colorScheme.cryptoSuccess,
            )
            Text(
                text = AuthConstants.Onboarding.SAMPLE_HIGH,
                style = NumericType.Medium,
                color = MaterialTheme.colorScheme.cryptoError,
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Crypto.spreadTrack)
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.cryptoSuccess,
                                    MaterialTheme.colorScheme.cryptoError,
                                ),
                            ),
                        shape = RoundedCornerShape(Dimensions.Radius.full),
                    ),
            content = {},
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = AuthConstants.Onboarding.SAMPLE_SPREAD,
                style = NumericType.Caption,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.accentSoft,
                            shape = RoundedCornerShape(Dimensions.Radius.full),
                        ).padding(
                            horizontal = Dimensions.Spacing.sm,
                            vertical = Dimensions.Spacing.xxs,
                        ),
            )
        }
    }
}
