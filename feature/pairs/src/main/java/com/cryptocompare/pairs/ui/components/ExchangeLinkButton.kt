package com.cryptocompare.pairs.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cryptocompare.helpers.isSafeWebUrl
import com.cryptocompare.helpers.openExternalUrl
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.CryptoCompareThemePreview
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.ThemePreviews
import com.cryptocompare.ui.theme.borderPrimary

/**
 * Переход на биржу по реферальной ссылке.
 *
 * Компонент сам решает, рисоваться ли: при пустой или небезопасной ссылке не
 * возвращает ничего. Иначе каждое место вызова повторяло бы одну и ту же
 * проверку, а забытая проверка — это уже дыра, потому что адрес приходит
 * с бэкенда.
 *
 * Контурная, а не залитая: сплошная акцентная плашка внизу карточки перекричала
 * бы цены, ради которых экран существует. Акцент остаётся в тексте и рамке —
 * этого хватает, чтобы кнопка читалась как действие.
 */
@Composable
internal fun ExchangeLinkButton(
    exchangeName: String?,
    referralUrl: String?,
    modifier: Modifier = Modifier,
    onOpenFailed: () -> Unit = {},
) {
    if (!referralUrl.isSafeWebUrl()) return

    val context = LocalContext.current
    val label =
        if (exchangeName.isNullOrBlank()) {
            // «Открыть на неизвестно» звучало бы поломкой, а ссылку терять незачем
            stringResource(R.string.pair_detail_open_exchange_site)
        } else {
            stringResource(R.string.pair_detail_open_on_exchange, exchangeName)
        }

    OutlinedButton(
        onClick = {
            if (!context.openExternalUrl(referralUrl)) onOpenFailed()
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(Dimensions.Height.button),
        shape = MaterialTheme.shapes.small,
        border =
            androidx.compose.foundation.BorderStroke(
                width = Dimensions.Border.thin,
                color = MaterialTheme.colorScheme.borderPrimary,
            ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            modifier =
                Modifier
                    .padding(start = Dimensions.Gap.xs)
                    .size(Dimensions.IconSize.sm),
        )
    }
}

@ThemePreviews
@Composable
private fun ExchangeLinkButtonPreview() {
    CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.screen),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        ) {
            ExchangeLinkButton(
                exchangeName = "binance",
                referralUrl = "https://accounts.binance.com/register?ref=X1",
            )
            ExchangeLinkButton(
                exchangeName = null,
                referralUrl = "https://gate.io/signup",
            )
            // ниже не должно нарисоваться ничего
            ExchangeLinkButton(exchangeName = "mexc", referralUrl = null)
            ExchangeLinkButton(exchangeName = "mexc", referralUrl = "intent://evil#Intent;end")
        }
    }
}
