package com.cryptocompare.pairs.util

import com.cryptocompare.model.provider.ProviderDetail
import com.cryptocompare.model.ticker.TickerPrice

/**
 * Обновляет цены бирж живым тиком. Тик по паре приходит на каждого провайдера
 * отдельно, поэтому совпадение ищем по id: у совпавшего заменяем обе цены,
 * остальных не трогаем.
 */
internal fun List<ProviderDetail>.withLivePrices(tick: TickerPrice): List<ProviderDetail> =
    map { detail ->
        if (detail.provider.id == tick.providerId) {
            detail.copy(priceSell = tick.priceSell, priceBuy = tick.priceBuy)
        } else {
            detail
        }
    }
