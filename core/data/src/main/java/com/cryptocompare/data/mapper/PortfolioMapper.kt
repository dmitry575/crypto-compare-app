package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.dao.PortfolioPositionWithExchange
import com.cryptocompare.data.local.dao.SymbolSellPrice
import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import com.cryptocompare.data.local.entity.PortfolioQuoteEntity
import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioQuote
import com.cryptocompare.model.symbol.SymbolSellQuote

fun PortfolioPositionWithExchange.toDomain(): PortfolioPosition =
    PortfolioPosition(
        symbolId = position.symbolId,
        ticker = position.ticker,
        amount = position.amount,
        buyPrice = position.buyPrice,
        updatedAtMillis = position.updatedAtMillis,
        providerId = position.providerId,
        exchangeName = exchangeName?.takeIf { it.isNotBlank() },
    )

fun PortfolioPosition.toEntity(): PortfolioPositionEntity =
    PortfolioPositionEntity(
        symbolId = symbolId,
        ticker = ticker.trim().uppercase(),
        amount = amount,
        buyPrice = buyPrice,
        updatedAtMillis = updatedAtMillis,
        providerId = providerId,
    )

fun PortfolioQuote.toEntity(): PortfolioQuoteEntity =
    PortfolioQuoteEntity(
        symbolId = symbolId,
        providerId = providerId,
        price = price,
        quotedAtMillis = quotedAtMillis,
    )

/** Строки без годной цены пропускаются: ноль или NaN в портфеле значили бы «позиция обесценилась». */
fun List<SymbolSellPrice>.toSellQuotes(): Map<Long, SymbolSellQuote> =
    mapNotNull { row ->
        row.sellPrice.validPriceOrNull()?.let { price ->
            row.symbolId to
                SymbolSellQuote(
                    price = price,
                    providerId = row.providerId,
                    exchangeName = row.providerName,
                )
        }
    }.toMap()
