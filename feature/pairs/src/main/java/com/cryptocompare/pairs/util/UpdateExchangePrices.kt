package com.cryptocompare.pairs.util

import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerPrice

/**
 * Обновляет цены бирж живым тиком. Тик по паре приходит на каждого провайдера
 * отдельно, поэтому совпадение ищем по id: у совпавшего заменяем обе цены,
 * остальных не трогаем.
 *
 * Цены тика проходят ту же проверку, что и REST: сокет присылает ноль на месте
 * отсутствующей стороны, и без проверки строка, загруженная с прочерком, через
 * полсекунды показывала бы «0».
 */
internal fun List<ProviderDetail>.withLivePrices(tick: TickerPrice): List<ProviderDetail> =
    map { detail ->
        if (detail.provider.id == tick.providerId) {
            detail.copy(
                priceSell = tick.priceSell.validPriceOrNull(),
                priceBuy = tick.priceBuy.validPriceOrNull(),
            )
        } else {
            detail
        }
    }
