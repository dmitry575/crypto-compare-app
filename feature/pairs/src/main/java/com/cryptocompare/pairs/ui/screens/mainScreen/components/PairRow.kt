package com.cryptocompare.pairs.ui.screens.mainScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cryptocompare.helpers.isNotableSpread
import com.cryptocompare.helpers.parseTicker
import com.cryptocompare.helpers.toCompactPriceString
import com.cryptocompare.helpers.toCompactVolumeString
import com.cryptocompare.helpers.toPercentString
import com.cryptocompare.helpers.util.PriceFormatConstants
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.components.Change24hLabel
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.ui.theme.CryptoCompareThemePreview
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.NumericType
import com.cryptocompare.ui.theme.ThemePreviews
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Строка каталога.
 *
 * Колонок «Max/Min» больше нет: две цены одинакового веса заставляли вычитать
 * одно из другого в уме. Теперь видно цену и готовый спред — насколько широк
 * рынок по этой паре.
 *
 * Иерархия: цена главная, изменение за 24ч стоит прямо под ней и остаётся
 * единственным цветным элементом строки. Спред и объём съезжают в один
 * приглушённый ряд слева. Раньше изменение и плашка спреда стояли рядом — два
 * процента одинакового вида, означающие совершенно разное.
 *
 * Бюджет ширин на 360dp. Внутри строки 296dp: 360 минус поля экрана 2×16 и
 * паддинг строки 2×16. Из них значок 36, зазор 12, зазор 12, колонка цены ~76
 * и звезда 48 — левой колонке остаётся ~112dp. Зазора перед звездой намеренно
 * нет: у IconButton свои отступы вокруг иконки 24dp, просвет и так виден, а
 * 12dp уходят туда, где их не хватало. Приглушённый ряд «0.42% · 98.75M» при
 * 12sp занимает ~101dp, то есть влезает с запасом в 11dp и не более того.
 *
 * Кто соберётся ставить сюда спарклайн: он потребует ~44dp плюс зазор, и вместе
 * с объёмом они уже не помещаются. Платить придётся объёмом.
 */
@Composable
fun PairRow(
    pair: PairUiItem,
    modifier: Modifier = Modifier,
    minRowHeight: Dp? = null,
    showVolume: Boolean = true,
    isFavourite: Boolean = false,
    onFavouriteClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val parts = remember(pair.ticker) { pair.ticker.parseTicker() }
    val base = parts?.first ?: pair.ticker
    val rowModifier = if (minRowHeight != null) modifier.heightIn(min = minRowHeight) else modifier

    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Dimensions.Padding.listItemHorizontal,
                    vertical = Dimensions.Padding.listItemVertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PairBadge(base = base)

        Spacer(modifier = Modifier.width(Dimensions.Gap.md))

        // числа бирж здесь нет: каталог отдаёт одну строку на тикер с общим
        // providerId, и любой такой счётчик всегда показывал бы единицу
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = tickerLabel(pair.ticker, parts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = marketLabel(pair, showVolume),
                style = NumericType.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(Dimensions.Gap.md))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xxs),
        ) {
            Text(
                text = pair.maxPrice.toCompactPriceString(),
                style = NumericType.Small,
                color = MaterialTheme.colorScheme.textPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Change24hLabel(change24h = pair.change24h)
        }

        IconButton(
            onClick = onFavouriteClick,
            modifier = Modifier.size(Dimensions.TouchTarget.min),
        ) {
            Icon(
                imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription =
                    stringResource(
                        if (isFavourite) {
                            R.string.pairs_remove_from_favorites
                        } else {
                            R.string.pairs_add_to_favorites
                        },
                    ),
                tint =
                    if (isFavourite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.textTertiary
                    },
            )
        }
    }
}

/** Котировка приглушена: одинаковый USDT в каждой строке не должен спорить с базой. */
@Composable
private fun tickerLabel(
    ticker: String,
    parts: Pair<String, String>?,
): AnnotatedString {
    val quoteColor = MaterialTheme.colorScheme.textTertiary
    return remember(ticker, parts, quoteColor) {
        if (parts == null) {
            AnnotatedString(ticker)
        } else {
            buildAnnotatedString {
                append(parts.first)
                withStyle(SpanStyle(color = quoteColor)) {
                    append("/")
                    append(parts.second)
                }
            }
        }
    }
}

/**
 * Приглушённый ряд: спред, затем объём.
 *
 * Спред красится акцентом, только когда он заметный: зелёный и красный в
 * приложении означают направление цены, а широкий рынок это не «подорожало».
 * Ниже порога разброс тонет в комиссиях и уходит в приглушённый цвет.
 *
 * [showVolume] решается один раз на экран, а не построчно: ширина у всех строк
 * одна, и объём, пропадающий от строки к строке, читался бы как сбой данных.
 * Когда места нет, объём убирается целиком — «98.7…» это не число, а спред
 * остаётся всегда.
 */
@Composable
private fun marketLabel(
    pair: PairUiItem,
    showVolume: Boolean,
): AnnotatedString {
    val spreadColor =
        if (pair.spreadPercent.isNotableSpread()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.textTertiary
        }
    val mutedColor = MaterialTheme.colorScheme.textTertiary

    return remember(pair.spreadPercent, pair.quoteVolume24h, showVolume, spreadColor, mutedColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = spreadColor)) {
                append(pair.spreadPercent.toPercentString())
            }
            if (showVolume) {
                withStyle(SpanStyle(color = mutedColor)) {
                    append(PairsConstants.MainScreen.META_SEPARATOR)
                    append(
                        pair.quoteVolume24h?.toCompactVolumeString()
                            ?: PriceFormatConstants.NON_FINITE_PLACEHOLDER,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PairRowPreview() {
    PairRowPreviewContent(showVolume = true)
}

@Preview(name = "Узкий экран", widthDp = 320)
@Preview(name = "Крупный шрифт", fontScale = 1.3f)
@Composable
private fun PairRowNarrowPreview() {
    PairRowPreviewContent(showVolume = false)
}

@Composable
private fun PairRowPreviewContent(showVolume: Boolean) {
    CryptoCompareThemePreview(darkTheme = isSystemInDarkTheme()) {
        Column {
            previewPairs().forEach { pair ->
                PairRow(
                    pair = pair,
                    minRowHeight = Dimensions.Height.listItemStats,
                    showVolume = showVolume,
                    isFavourite = pair.ticker == "ETHUSDT",
                )
            }
        }
    }
}

private fun previewPairs(): List<PairUiItem> =
    listOf(
        previewPair("BTCUSDT", 82_145.30, 0.42, 98_750_000.0, 2.35),
        previewPair("ETHUSDT", 3_201.44, 0.03, 41_200_000.0, -1.2),
        previewPair("SOLUSDT", 184.07, 0.0, null, null),
        previewPair("1000SATSUSDT", 0.000000331, 1.18, 1_230_000_000_000.0, 0.0),
    )

private fun previewPair(
    ticker: String,
    price: Double,
    spread: Double,
    volume: Double?,
    change: Double?,
) = PairUiItem(
    ticker = ticker,
    symbolIds = emptyList(),
    providerIds = emptyList(),
    minPrice = price,
    maxPrice = price,
    spreadPercent = spread,
    quoteVolume24h = volume,
    change24h = change,
)
