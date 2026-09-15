package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.isNotableSpread
import com.cryptocompare.helpers.spreadPercent
import com.cryptocompare.helpers.toPercentString
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.pairs.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.OverlineType
import com.cryptocompare.ui.theme.accentSoft
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Разница между биржами — то, ради чего приложение и существует.
 *
 * Раньше на этом месте стояли «Lowest ask» и «Highest bid» двумя колонками.
 * Числа брались с разных бирж и разного порядка, стояли рядом без связи между
 * собой и читались как поломка. Здесь у каждого края есть имя биржи, а снизу —
 * готовая разница: покупаешь слева, продаёшь справа.
 *
 * **Пара берётся с бэкенда** — [bestPair], та же, что в строке каталога и в
 * выжимке экрана сравнения. Раньше блок искал её сам по [exchanges], а разбивка
 * по биржам приходит без фильтра свежести: на `ethusdc` 2026-09-14 он показывал
 * «купить на bingx, продать на kraken, +0.87%», потому что цена kraken стояла,
 * тогда как каталог и сравнение для той же пары показывали +0.078%. Из
 * [exchanges] здесь берутся только имена бирж.
 *
 * Знак значим и в норме отрицателен — купить дороже, чем продать, это обычное
 * состояние рынка. Если бэкенд лучшую пару не прислал, а биржа одна, блок
 * показывает её собственный bid/ask: выбирать не из чего, и ответ очевиден.
 * Если бирж несколько, блок так и говорит, что лучших цен нет, но остаётся на
 * месте — он же ведёт на экран сравнения.
 *
 * Цвета краёв нейтральные. Зелёный на дешёвой стороне против красного на
 * дорогой работал, пока разница считалась модулем; со знаком продажа сплошь и
 * рядом оказывается ниже покупки, и градиент начинает врать. Выделяется только
 * сам процент, и только когда он вышел в плюс.
 */
@Composable
internal fun SpreadBar(
    bestPair: TickerBestPrice?,
    exchanges: List<ProviderDetail>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (exchanges.isEmpty()) return

    val crossExchange = exchanges.size > 1
    val single = exchanges.singleOrNull()

    // priceSell одной биржи — это ask, по нему пользователь покупает; priceBuy — bid.
    // В разбивке по биржам имена от лица биржи, в лучшей паре — от лица пользователя
    val buyPrice = bestPair?.bestAskPrice ?: single?.priceSell
    val sellPrice = bestPair?.bestBidPrice ?: single?.priceBuy
    val buyExchange =
        bestPair?.bestAskProviderId?.let { id -> exchanges.firstOrNull { it.provider.id == id } } ?: single
    val sellExchange =
        bestPair?.bestBidProviderId?.let { id -> exchanges.firstOrNull { it.provider.id == id } } ?: single

    val percent = bestPair?.spreadPercent ?: spreadPercent(buyPrice = buyPrice, sellPrice = sellPrice)
    val difference = if (buyPrice != null && sellPrice != null) sellPrice - buyPrice else null

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.Radius.card))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .background(
                    color = MaterialTheme.colorScheme.bgCard,
                    shape = RoundedCornerShape(Dimensions.Radius.card),
                ).border(
                    width = Dimensions.Border.card,
                    color = MaterialTheme.colorScheme.borderPrimary,
                    shape = RoundedCornerShape(Dimensions.Radius.card),
                ).padding(Dimensions.Padding.cardLarge),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(
                        if (crossExchange) {
                            R.string.pair_detail_spread_title
                        } else {
                            R.string.pair_detail_spread_title_single
                        },
                    ),
                style = OverlineType,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            // шеврон, только когда блок ведёт дальше: без него карточка выглядит
            // кликабельной там, где никуда не ведёт
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.pair_detail_open_comparison),
                    tint = MaterialTheme.colorScheme.textTertiary,
                    modifier = Modifier.size(Dimensions.IconSize.md),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            SpreadEnd(
                label =
                    if (crossExchange) {
                        stringResource(R.string.pair_detail_buy_on, exchangeName(buyExchange))
                    } else {
                        stringResource(R.string.pair_detail_buy_price)
                    },
                price = buyPrice?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                alignment = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            SpreadEnd(
                label =
                    if (crossExchange) {
                        stringResource(R.string.pair_detail_sell_on, exchangeName(sellExchange))
                    } else {
                        stringResource(R.string.pair_detail_sell_price)
                    },
                price = sellPrice?.toPriceString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                alignment = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Crypto.spreadTrack)
                    .background(
                        color = MaterialTheme.colorScheme.accentSoft,
                        shape = RoundedCornerShape(Dimensions.Radius.full),
                    ),
            content = {},
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (difference != null) {
                        stringResource(R.string.pair_detail_spread_difference, difference.toPriceString())
                    } else {
                        stringResource(R.string.pair_detail_no_best)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = percent?.toPercentString() ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                style = NumericType.Medium,
                color =
                    if (percent != null && percent.isNotableSpread()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.textPrimary
                    },
            )
        }
    }
}

@Composable
private fun SpreadEnd(
    label: String,
    price: String,
    alignment: TextAlign,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        horizontalAlignment = if (alignment == TextAlign.End) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textTertiary,
            textAlign = alignment,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = price,
            style = NumericType.Medium,
            color = MaterialTheme.colorScheme.textPrimary,
            textAlign = alignment,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun exchangeName(detail: ProviderDetail?): String =
    detail?.provider?.name ?: stringResource(R.string.pair_detail_unknown_exchange)
