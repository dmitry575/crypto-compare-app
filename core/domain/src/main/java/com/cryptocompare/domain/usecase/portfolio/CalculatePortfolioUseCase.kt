package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.helpers.util.MoneyConstants
import com.cryptocompare.helpers.validPriceOrNull
import com.cryptocompare.model.portfolio.Portfolio
import com.cryptocompare.model.portfolio.PortfolioHolding
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioSummary
import com.cryptocompare.model.symbol.SymbolSellQuote
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Считает стоимость и прибыль портфеля по текущим ценам.
 *
 * Чистый расчёт: позиции и цены приходят параметрами, в репозитории use case
 * не ходит. Так его матрицу случаев — ноль количества, ноль вложенного,
 * отсутствующая цена, крупные суммы — можно прогнать тестами целиком (#42).
 *
 * Деньги считаются в `BigDecimal` по политике [MoneyConstants]: у `Double`
 * 0.1 × 0.3 даёт 0.030000000000000002, и прибыль по позиции, которая стоит
 * ровно столько же, сколько за неё заплатили, выходила бы не нулём.
 *
 * Цена, которой нет, — это `null`, а не ноль: ноль означал бы, что позиция
 * обесценилась, и портфель показывал бы убыток в 100% всякий раз, когда символ
 * выпал из каталога.
 */
class CalculatePortfolioUseCase
    @Inject
    constructor() {
        /** [quotes] — цены продажи по `symbolId` с биржей, которая их даёт; символа может и не быть. */
        operator fun invoke(
            positions: List<PortfolioPosition>,
            quotes: Map<Long, SymbolSellQuote>,
        ): Portfolio {
            val holdings = positions.map { position -> holding(position, quotes[position.symbolId]) }

            return Portfolio(holdings = holdings, summary = summarize(holdings))
        }

        private fun holding(
            position: PortfolioPosition,
            quote: SymbolSellQuote?,
        ): PortfolioHolding {
            val currentPrice = quote?.price.validPriceOrNull()
            val amount = position.amount.toDecimal()
            val invested = amount.multiply(position.buyPrice.toDecimal())
            val currentValue = currentPrice?.let { price -> amount.multiply(price.toDecimal()) }
            val profit = currentValue?.subtract(invested)

            return PortfolioHolding(
                position = position,
                currentPrice = currentPrice,
                priceExchange = priceExchange(position, quote, currentPrice),
                invested = invested.toDouble(),
                currentValue = currentValue?.toDouble(),
                profit = profit?.toDouble(),
                profitPercent = profit?.percentOf(invested),
            )
        }

        /**
         * У позиции с биржей это всегда её биржа, даже без цены: «на Bybit, цены
         * пока нет» — честный ответ, и пользователь видит, что оценка не уехала на
         * чужую площадку. У позиции без биржи — биржа лучшего bid, и только когда
         * её цена годится: иначе на экране стоял бы прочерк «по цене bitget».
         */
        private fun priceExchange(
            position: PortfolioPosition,
            quote: SymbolSellQuote?,
            currentPrice: Double?,
        ): String? {
            if (position.providerId != null) return position.exchangeName ?: quote?.exchangeName

            return quote?.exchangeName?.takeIf { currentPrice != null }
        }

        /** Итог — только по тому, что удалось оценить; остальное экран покажет отдельно. */
        private fun summarize(holdings: List<PortfolioHolding>): PortfolioSummary? {
            val priced = holdings.filter { it.currentValue != null }
            if (priced.isEmpty()) return null

            val invested = priced.sumOfDecimal { it.invested }
            val currentValue = priced.sumOfDecimal { it.currentValue ?: 0.0 }
            val profit = currentValue.subtract(invested)

            return PortfolioSummary(
                invested = invested.toDouble(),
                currentValue = currentValue.toDouble(),
                profit = profit.toDouble(),
                profitPercent = profit.percentOf(invested),
                positionsWithoutPrice = holdings.size - priced.size,
            )
        }

        private fun BigDecimal.percentOf(base: BigDecimal): Double? {
            if (base.signum() == 0) return null

            return divide(base, MoneyConstants.DIVISION)
                .multiply(BigDecimal.valueOf(MoneyConstants.PERCENT_MULTIPLIER))
                .toDouble()
        }

        private fun List<PortfolioHolding>.sumOfDecimal(value: (PortfolioHolding) -> Double): BigDecimal =
            fold(BigDecimal.ZERO) { sum, holding -> sum.add(value(holding).toDecimal()) }

        /** `BigDecimal.valueOf` не принимает NaN и бесконечность, а в базе может лежать что угодно. */
        private fun Double.toDecimal(): BigDecimal = if (isFinite()) BigDecimal.valueOf(this) else BigDecimal.ZERO
    }
