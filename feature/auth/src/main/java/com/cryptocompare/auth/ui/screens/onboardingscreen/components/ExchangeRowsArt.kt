package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Одна пара на трёх биржах — то, ради чего приложение и написано.
 *
 * Общее начало цены приглушено, расходящийся хвост контрастный: разница видна
 * типографикой раньше, чем её объясняет подпись под иллюстрацией.
 */
@Composable
internal fun ExchangeRowsArt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xs),
    ) {
        AuthConstants.Onboarding.SAMPLE_QUOTES.forEach { (prefix, tail) ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.bgCard,
                            shape = RoundedCornerShape(Dimensions.Radius.md),
                        ).border(
                            width = Dimensions.Border.card,
                            color = MaterialTheme.colorScheme.borderPrimary,
                            shape = RoundedCornerShape(Dimensions.Radius.md),
                        ).padding(
                            horizontal = Dimensions.Padding.cardMedium,
                            vertical = Dimensions.Padding.cardSmall,
                        ),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .size(Dimensions.IconSize.chipDot)
                            .background(MaterialTheme.colorScheme.statusActive, CircleShape),
                    content = {},
                )

                // имя биржи не пишем: иллюстрация не должна рекламировать площадку
                Row(
                    modifier =
                        Modifier
                            .width(AuthConstants.Onboarding.artNameWidth)
                            .height(AuthConstants.Onboarding.artNameHeight)
                            .background(
                                MaterialTheme.colorScheme.borderPrimary,
                                RoundedCornerShape(Dimensions.Radius.sm),
                            ),
                    content = {},
                )

                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.textTertiary)) {
                                append(prefix)
                            }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.textPrimary)) {
                                append(tail)
                            }
                        },
                    style = NumericType.Small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
