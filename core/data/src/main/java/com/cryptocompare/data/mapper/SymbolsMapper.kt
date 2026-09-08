package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.entity.SymbolEntity
import com.cryptocompare.model.symbol.Symbol
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolBestPriceDto
import com.cryptocompare.network.dto.apiDTO.cryptoCompareDTO.SymbolDto

/**
 * Строка каталога с бэкенда в строку локальной таблицы.
 *
 * Обратного маппинга (сущность в [Symbol]) больше нет. `Symbol` описывает
 * котировку **одной биржи**, а строка каталога — лучшую пару по тикеру, где
 * стороны взяты с разных бирж. Пока такой маппинг существовал, оффлайн-режим
 * детального экрана показывал одну выдуманную биржу с чужими ценами.
 */
fun SymbolBestPriceDto.toEntityFromDto(syncedAtMillis: Long): SymbolEntity =
    SymbolEntity(
        id = id,
        ticker = ticker,
        symbol = symbol,
        bestAskProviderId = bestAskProviderId,
        bestAskPrice = bestAskPrice,
        bestBidProviderId = bestBidProviderId,
        bestBidPrice = bestBidPrice,
        spreadPercent = spreadPercent,
        bestAskUpdatedAt = bestAskUpdatedAt,
        bestBidUpdatedAt = bestBidUpdatedAt,
        updatedAt = updatedAt.orEmpty(),
        syncedAtMillis = syncedAtMillis,
        change24h = change24h,
        quoteVolume24h = quoteVolume24h,
        volume24h = volume24h,
    )

fun List<SymbolBestPriceDto>.toEntityFromDto(syncedAtMillis: Long): List<SymbolEntity> =
    map { it.toEntityFromDto(syncedAtMillis) }

/** Котировка одной биржи: приходит с разбивки по биржам, в каталоге не лежит. */
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
