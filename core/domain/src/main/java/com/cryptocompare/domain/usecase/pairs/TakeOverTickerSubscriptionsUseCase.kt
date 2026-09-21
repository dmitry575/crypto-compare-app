package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.toSubscriptionSet
import com.cryptocompare.helpers.util.WebSocketConstants
import javax.inject.Inject

/**
 * Экран забирает подписки соединения себе: деталям и сравнению нужен один
 * тикер, портфелю — тикеры его позиций, а слотов у соединения мало.
 *
 * Набор каталога репозиторий сохраняет сам и вернёт при уходе последнего
 * захватившего экрана — см. [RestoreTickerSubscriptionsUseCase]. Лишнее сверх
 * лимита отсекается здесь: девятая подписка возвращает ошибку без указания
 * тикера, и разбирать её постфактум не по чему.
 */
class TakeOverTickerSubscriptionsUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
    ) {
        operator fun invoke(tickers: Set<String>) {
            tickerStreamRepository.beginTickerTakeover(
                tickers.toSubscriptionSet(WebSocketConstants.MAX_SUBSCRIPTIONS),
            )
        }
    }
