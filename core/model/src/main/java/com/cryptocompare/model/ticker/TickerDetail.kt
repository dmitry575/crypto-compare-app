package com.cryptocompare.model.ticker

import com.cryptocompare.model.provider.ProviderDetail

/**
 * Котировки пары по биржам — одного символа, то есть одного набора сетей.
 *
 * Символов у тикера бывает несколько: USDC в Ethereum и USDC в Solana — разные
 * активы, и в одну таблицу их биржи не сводятся, иначе сравнение показывало бы
 * спреды в тысячи процентов.
 */
data class TickerDetail(
    val ticker: String,
    val exchanges: List<ProviderDetail>,
    /** Символ, чьи это биржи. `null` — открыт тикер без выбора символа. */
    val symbolId: Long? = null,
    /** Сети символа в едином виде, крупные сначала. Пусто — бэкенд их не прислал. */
    val networks: List<String> = emptyList(),
)
