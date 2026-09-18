package com.cryptocompare.model.portfolio

/**
 * То, что ввёл пользователь в форме: без времени изменения — его ставит слой
 * данных, а не экран.
 */
data class PortfolioPositionDraft(
    val symbolId: Long,
    val ticker: String,
    val amount: Double,
    val buyPrice: Double,
)
