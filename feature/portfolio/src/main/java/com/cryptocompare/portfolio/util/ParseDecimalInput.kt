package com.cryptocompare.portfolio.util

/**
 * Число из поля формы: «0,42», «0.42», «72 000», «72 000,5».
 *
 * Запятая и точка обе означают дробную часть: русская раскладка даёт запятую,
 * а скопированная с биржи цена приходит с точкой. Пробелы между разрядами
 * выбрасываются — «72 000» пишут чаще, чем «72000». Всё остальное — не число,
 * и форма не должна угадывать, что имелось в виду.
 *
 * `null` для пустого поля и для мусора; отрицательные числа форме не нужны и
 * тоже не принимаются.
 */
fun String.parseDecimalInput(): Double? {
    val cleaned =
        filterNot { it in PortfolioConstants.Input.GROUPING_CHARS }
            .replace(PortfolioConstants.Input.DECIMAL_COMMA, PortfolioConstants.Input.DECIMAL_DOT)

    if (cleaned.isEmpty() || cleaned.count { it == PortfolioConstants.Input.DECIMAL_DOT } > 1) return null
    if (!cleaned.all { it.isDigit() || it == PortfolioConstants.Input.DECIMAL_DOT }) return null

    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}
