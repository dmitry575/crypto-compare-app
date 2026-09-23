package com.cryptocompare.model.error

/** Почему введённое не принято — у каждой причины свой текст. */
enum class ValidationErrorReason {
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_WEAK,
    PASSWORDS_DO_NOT_MATCH,

    /** Количество или цена позиции ниже нуля. */
    NEGATIVE_VALUES,
}
