package com.cryptocompare.data.mapper

import com.cryptocompare.data.local.entity.PortfolioPositionEntity
import com.cryptocompare.model.portfolio.PortfolioPosition

fun PortfolioPositionEntity.toDomain(): PortfolioPosition =
    PortfolioPosition(
        symbolId = symbolId,
        ticker = ticker,
        amount = amount,
        buyPrice = buyPrice,
        updatedAtMillis = updatedAtMillis,
    )

fun PortfolioPosition.toEntity(): PortfolioPositionEntity =
    PortfolioPositionEntity(
        symbolId = symbolId,
        ticker = ticker.trim().uppercase(),
        amount = amount,
        buyPrice = buyPrice,
        updatedAtMillis = updatedAtMillis,
    )
