package com.cryptocompare.data.local.dao

import androidx.room.Embedded
import com.cryptocompare.data.local.entity.PortfolioPositionEntity

/**
 * Позиция вместе с именем своей биржи из справочника. Имя нужно и строке
 * портфеля, и форме, в том числе без сети и до первой цены.
 */
data class PortfolioPositionWithExchange(
    @Embedded
    val position: PortfolioPositionEntity,
    /** `null` — биржа у позиции не указана или её ещё нет в справочнике. */
    val exchangeName: String?,
)
