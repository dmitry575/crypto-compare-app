package com.cryptocompare.portfolio.viewmodel.portfolioviewmodel

import com.cryptocompare.model.portfolio.PortfolioHolding
import com.cryptocompare.model.portfolio.PortfolioSummary

data class PortfolioUiState(
    val holdings: List<PortfolioHolding> = emptyList(),
    /** `null` — считать итог не из чего: портфель пуст или цен нет ни по одной позиции. */
    val summary: PortfolioSummary? = null,
    /** Пока база не ответила, пустой список — это ещё не «портфель пуст». */
    val loading: Boolean = true,
    /**
     * Цены замерли: поток лежит дольше `WebSocketConstants.STALE_NOTICE_DELAY_MS`.
     * Числа на экране остаются — старая цена полезнее прочерка, — но выглядят они
     * так же, как живые, и без полоски это было бы враньём.
     */
    val isStale: Boolean = false,
    /** На каком времени цены замерли; `null` — сказать нечего (каталога ещё не было). */
    val lastUpdateMillis: Long? = null,
    /** «Обновить» не достал ни одной цены — экран скажет об этом один раз. */
    val refreshFailed: Boolean = false,
)
