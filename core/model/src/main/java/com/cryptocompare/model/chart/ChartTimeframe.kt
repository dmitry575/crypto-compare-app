package com.cryptocompare.model.chart

/**
 * Масштаб свечей на графике. Доменное понятие: как это ложится на эндпоинты
 * стороннего API, знает только слой данных.
 */
enum class ChartTimeframe {
    M15,
    H1,
    H4,
    D1,
    W1,
    ;

    companion object {
        val DEFAULT: ChartTimeframe = D1
    }
}
