package com.cryptocompare.model.portfolio

/**
 * Портфель целиком: позиции со своей стоимостью и общий итог.
 *
 * [summary] — `null`, когда считать нечего: портфель пуст или цены нет ни у
 * одной позиции. Пустой итог из нулей был бы враньём — «вложено 0, прибыль 0»
 * выглядит как честный ответ, хотя означает «мы не знаем».
 */
data class Portfolio(
    val holdings: List<PortfolioHolding> = emptyList(),
    val summary: PortfolioSummary? = null,
)
