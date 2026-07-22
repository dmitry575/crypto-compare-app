package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.entity.ProviderEntity
import com.cryptocompare.model.provider.Provider
import com.cryptocompare.model.provider.ProviderStatus
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.ProviderDto

fun ProviderEntity.toDomainFromEntity(): Provider =
    Provider(
        id = id,
        name = name,
        webSite = website,
        status = ProviderStatus.valueOf(status),
    )

fun ProviderDto.toEntityFromDto(syncedAtMillis: Long): ProviderEntity =
    ProviderEntity(
        id = id,
        name = name,
        website = webSite,
        status = status.name,
        syncedAtMillis = syncedAtMillis,
    )

fun List<ProviderEntity>.toDomainFromEntity(): List<Provider> = map(ProviderEntity::toDomainFromEntity)

fun List<ProviderDto>.toEntityFromDto(syncedAtMillis: Long): List<ProviderEntity> =
    map {
        it.toEntityFromDto(syncedAtMillis)
    }

fun String.toCryptoCompareExchangeName(): String =
    when (lowercase().trim()) {
        "mexc", "mexc.com" -> "MEXC"
        "xtcom", "xt.com", "xt" -> "XT"
        "bybit", "bybit.com" -> "Bybit"
        "bingx", "bingx.com" -> "BingX"
        "bitget", "bitget.com" -> "Bitget"
        "gateio", "gate.io", "gate" -> "GateIO"
        "bitmart", "bitmart.com" -> "BitMart"
        else -> replaceFirstChar { it.uppercase() }
    }
