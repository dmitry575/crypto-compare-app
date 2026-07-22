package com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO

import com.cryptocompare.model.provider.ProviderStatus

data class GetProviderResponse(
    val errorCode: Int,
    val errorMsgs: List<String>?,
    val id: Int,
    val name: String?,
    val webSite: String?,
    val baseUrl: String?,
    val accessKey: String?,
    val secretKey: String?,
    val status: ProviderStatus,
    val createdAt: String,
    val updatedAt: String,
)
