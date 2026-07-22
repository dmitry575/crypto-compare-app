package com.cryptocompare.model.provider

data class ProviderDetail(
    val provider: Provider,
    val priceSell: Double?,
    val priceBuy: Double?,
)
