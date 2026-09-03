package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.model.symbol.Symbol
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto

fun SymbolEntity.toDomainFromEntity(): Symbol =
    Symbol(
        id = id,
        ticker = ticker,
        symbol = symbol,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
        volume24h = volume24h,
    )

fun SymbolDto.toEntityFromDto(syncedAtMillis: Long): SymbolEntity =
    SymbolEntity(
        id = id,
        ticker = ticker,
        symbol = symbol,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
        updatedAt = updatedAt,
        syncedAtMillis = syncedAtMillis,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
        volume24h = volume24h,
    )

fun List<SymbolDto>.toEntityFromDto(syncedAtMillis: Long): List<SymbolEntity> =
    map {
        it.toEntityFromDto(syncedAtMillis)
    }

fun List<SymbolEntity>.toDomainFromEntity(): List<Symbol> = map(SymbolEntity::toDomainFromEntity)

fun SymbolDto.symbolToDomainFromDto(): Symbol =
    Symbol(
        id = id,
        ticker = ticker,
        symbol = symbol,
        providerId = providerId,
        priceSell = priceSell,
        priceBuy = priceBuy,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
        volume24h = volume24h,
    )

fun List<SymbolDto>.symbolToDomainFromDto(): List<Symbol> = map(SymbolDto::symbolToDomainFromDto)
