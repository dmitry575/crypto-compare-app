package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

data class SymbolDto(
    val id: Long,
    val ticker: String?,
    val symbol: String?,
    val providerId: Int,
    val priceSell: Double,
    val priceBuy: Double,
    val updatedAt: String,
    val volume24h: Double? = null,
    val quoteVolume24h: Double? = null,
    val change24h: Double? = null,
    /**
     * Сети символа строкой через запятую, в формате источника: `arbitrum,base,eth`,
     * `Arbitrum One,BNB Smart Chain`, `ETH-ERC20`. Разбирает `networkNames`.
     * У одной пары бывает несколько символов — по одному на набор сетей.
     */
    val network: String? = null,
)
