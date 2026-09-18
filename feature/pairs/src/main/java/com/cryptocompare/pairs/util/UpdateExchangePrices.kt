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
 *
 * Тик двигает и время котировки. Раньше время приходило только из REST при
 * открытии экрана, а тики его не трогали, — и через пять минут экран сравнения
 * гасил как устаревшие даже биржи, тикавшие каждую секунду; белыми они
 * становились только после перезахода. Своего времени у события типа 4 нет,
 * поэтому берётся время получения: бэкенд шлёт событие, когда котировка
 * изменилась, и расхождение — задержка сети, против порога в минуты это ничто.
 */
internal fun List<ProviderDetail>.withLivePrices(
    tick: TickerPrice,
    receivedAtMillis: Long,
): List<ProviderDetail> =
    map { detail ->
        if (detail.provider.id == tick.providerId) {
            detail.copy(
                priceSell = tick.priceSell.validPriceOrNull(),
                priceBuy = tick.priceBuy.validPriceOrNull(),
                quotedAtMillis = receivedAtMillis,
            )
        } else {
            detail
        }
    }
