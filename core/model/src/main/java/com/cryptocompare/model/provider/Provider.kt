package com.cryptocompare.model.provider

data class Provider(
    val id: Int,
    val name: String?,
    val webSite: String?,
    val status: ProviderStatus,
)
