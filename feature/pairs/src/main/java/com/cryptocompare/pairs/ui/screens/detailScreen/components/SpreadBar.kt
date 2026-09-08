package com.cryptocompare.pairs.ui.screens.detailScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.isNotableSpread
import com.cryptocompare.helpers.spreadPercent
import com.cryptocompare.helpers.toPercentString
import com.cryptocompare.helpers.toPriceString
import com.cryptocompare.model.provider.ProviderDetail
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
 * Величина та же, что в строке каталога, и считает её общий `spreadPercent`.
 * Знак значим и в норме отрицателен — купить дороже, чем продать, это обычное
 * состояние рынка. При единственной бирже формула вырождается в её собственный
 * спред, отдельной ветки для этого не нужно.
 *
 * **Числу каталога оно при этом равно не всегда.** Здесь цены берутся из разбивки
 * по биржам, а каталог получает готовую пару из best-выдачи, и наборы бирж у этих
 * эндпоинтов разные: на BTCUSDT 2026-09-08 best-выдача выбрала биржи 19 и 23,
 * которых в разбивке в тот момент не было вовсе. Сводить экраны к одному источнику —
 * задача #19.
 *
 * Цвета краёв нейтральные. Зелёный на дешёвой стороне против красного на
 * дорогой работал, пока разница считалась модулем; со знаком продажа сплошь и
 * рядом оказывается ниже покупки, и градиент начинает врать. Выделяется только
 * сам процент, и только когда он вышел в плюс.
 */
@Composable
internal fun SpreadBar(
    exchanges: List<ProviderDetail>,
    modifier: Modifier = Modifier,
) {
    // priceSell — это ask, по нему пользователь покупает; priceBuy — bid, по нему продаёт.
    // Здесь строки пришли с разбивки по биржам, где имена от лица биржи
    val cheapest = exchanges.minByOrNull { it.priceSell ?: it.priceBuy ?: Double.MAX_VALUE }
    val dearest = exchanges.maxByOrNull { it.priceBuy ?: it.priceSell ?: Double.MIN_VALUE }

    val buyPrice = cheapest?.let { it.priceSell ?: it.priceBuy } ?: return
    val sellPrice = dearest?.let { it.priceBuy ?: it.priceSell } ?: return

    val percent = spreadPercent(buyPrice = buyPrice, sellPrice = sellPrice) ?: return
    val difference = sellPrice - buyPrice
    val crossExchange = exchanges.size > 1

    Column(
        modifier =
            modifier
                .fillMaxWidth()
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            SpreadEnd(
                label =
                    if (crossExchange) {
                        stringResource(R.string.pair_detail_buy_on, exchangeName(cheapest))
                    } else {
                        stringResource(R.string.pair_detail_buy_price)
                    },
                price = buyPrice.toPriceString(),
                alignment = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            SpreadEnd(
                label =
                    if (crossExchange) {
                        stringResource(R.string.pair_detail_sell_on, exchangeName(dearest))
                    } else {
                        stringResource(R.string.pair_detail_sell_price)
                    },
                price = sellPrice.toPriceString(),
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
                    stringResource(
                        R.string.pair_detail_spread_difference,
                        difference.toPriceString(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
            )
            Text(
                text = percent.toPercentString(),
                style = NumericType.Medium,
                color =
                    if (percent.isNotableSpread()) {
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
