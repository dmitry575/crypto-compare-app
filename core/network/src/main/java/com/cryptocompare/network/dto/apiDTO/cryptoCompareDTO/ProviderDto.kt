package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

import com.cryptocompare.model.provider.ProviderStatus

data class ProviderDto(
    val id: Int,
    val name: String?,
    val webSite: String?,
    val baseUrl: String?,
    val status: ProviderStatus,
)
