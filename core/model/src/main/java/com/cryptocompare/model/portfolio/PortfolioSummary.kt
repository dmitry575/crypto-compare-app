package com.cryptocompare.model.portfolio

/**
 * Итог по портфелю.
 *
 * Считается **только по позициям с известной ценой**. Иначе итог складывал бы
 * вложенное по всему портфелю со стоимостью его части, и прибыль выходила бы
 * тем больше отрицательной, чем больше символов выпало из каталога. Сколько
 * позиций осталось за итогом, говорит [positionsWithoutPrice] — экран об этом
 * предупреждает, а не молчит.
 */
data class PortfolioSummary(
    val invested: Double,
    val currentValue: Double,
    val profit: Double,
    /** `null`, когда вложено ноль: делить не на что. */
    val profitPercent: Double?,
    /** Позиции без цены: в итог они не вошли. */
    val positionsWithoutPrice: Int,
)
