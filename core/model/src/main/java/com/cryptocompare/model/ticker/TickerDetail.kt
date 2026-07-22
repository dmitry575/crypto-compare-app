package com.cryptocompare.model.ticker

import com.cryptocompare.model.provider.ProviderDetail

data class TickerDetail(
    val ticker: String,
    val exchanges: List<ProviderDetail>,
)
