package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.accentSoft

/**
 * Значок пары: базовая валюта на подложке. Логотипов монет у бэкенда нет,
 * а generic-иконка на каждой строке не помогает их различать — тикер помогает.
 *
 * Размер значка фиксированный, а шрифт растёт с системным масштабом, поэтому
 * текст подгоняется под значок: при масштабе 1.3 четыре знака уже не влезали,
 * и «1000BONK» читался как «100». При обычном шрифте размер остаётся
 * `NumericType.Caption` — подгонка только уменьшает и только когда не влезает.
 */
@Composable
internal fun PairBadge(
    base: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(Dimensions.Crypto.pairBadge)
                .background(
                    color = MaterialTheme.colorScheme.accentSoft,
                    shape = RoundedCornerShape(Dimensions.Radius.sm),
                ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            // ведущий «$» у мем-токенов не различает пары, а место занимает
            text =
                base
                    .removePrefix(PairsConstants.MainScreen.TICKER_PREFIX)
                    .take(PairsConstants.MainScreen.BADGE_MAX_CHARS),
            style = NumericType.Caption.copy(color = MaterialTheme.colorScheme.primary),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            // отступ входит в ширину, под которую подгоняется текст: без него
            // подогнанные четыре знака упираются в края подложки
            modifier = Modifier.padding(horizontal = Dimensions.Spacing.xxs),
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = PairsConstants.MainScreen.BADGE_MIN_FONT_SIZE,
                    maxFontSize = NumericType.Caption.fontSize,
                ),
        )
    }
}
