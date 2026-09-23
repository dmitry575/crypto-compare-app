package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import com.cryptocompare.domain.repository.TickerStreamRepository
import com.cryptocompare.helpers.util.WebSocketConstants
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Единственный писатель лучших пар из сокета в каталог.
 *
 * Раньше их писали ViewModel каталога и портфеля, каждая своей пачкой: при
 * портфеле, открытом поверх живого каталога, один и тот же тик уходил в базу
 * дважды, а Room дважды перечитывал видимые страницы. Экраны теперь только
 * читают базу, а пишет она одна — пока жив процесс.
 *
 * Тики копятся по `symbolId` и уходят в базу раз в
 * [WebSocketConstants.PRICE_FLUSH_INTERVAL_MS] (решение 2 в `CLAUDE.md`):
 * писать каждый — значит непрерывно дёргать Room и перерисовывать список.
 * Пачка живёт, пока тики идут: один пустой интервал — и таймер выходит, в фоне
 * без событий ничего не просыпается.
 *
 * Сбой записи не прерывает поток: следующая пачка запишется, а сам сбой
 * репозиторий уже отправил в отчёты.
 *
 * Не возвращается, пока его не отменят.
 */
class SyncLiveBestPricesUseCase
    @Inject
    constructor(
        private val tickerStreamRepository: TickerStreamRepository,
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        suspend operator fun invoke() {
            val lock = Mutex()
            val pending = mutableMapOf<Long, TickerBestPrice>()
            var flushScheduled = false

            coroutineScope {
                tickerStreamRepository.event.collect { event ->
                    if (event !is TickerStreamEvent.TickerBestPriceChange) return@collect

                    val startFlush =
                        lock.withLock {
                            pending[event.data.symbolId] = event.data
                            // флаг ставится и снимается под тем же замком, что и пачка:
                            // тик, пришедший после того, как таймер увидел пустоту,
                            // запустит новый таймер, а не повиснет до следующего тика
                            if (flushScheduled) {
                                false
                            } else {
                                flushScheduled = true
                                true
                            }
                        }
                    if (!startFlush) return@collect

                    launch {
                        while (true) {
                            delay(WebSocketConstants.PRICE_FLUSH_INTERVAL_MS.milliseconds)

                            val batch =
                                lock.withLock {
                                    if (pending.isEmpty()) {
                                        flushScheduled = false
                                        null
                                    } else {
                                        pending.values.toList().also { pending.clear() }
                                    }
                                } ?: return@launch

                            cryptoCompareRepository.applyBestPriceUpdates(batch)
                        }
                    }
                }
            }
        }
    }
