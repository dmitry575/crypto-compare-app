package com.cryptocompare.model.error

/**
 * Ошибка так, как её видит приложение, — без исключений конкретной сети или базы.
 *
 * Раньше по слоям ходил текст: `IllegalStateException` с сообщением бэкенда,
 * `exception.message` и английские строки прямо в ViewModel. Экран показывал
 * то, что ему дали, и русский интерфейс то и дело говорил «Unknown error» или
 * «db write failed». Теперь слой данных решает, **что** случилось, а текст на
 * языке интерфейса подбирает только UI.
 *
 * [isRetryable] говорит, есть ли смысл в «Повторить»: сеть вернётся, а
 * неверный пароль от повтора не исправится.
 */
sealed interface AppError {
    val isRetryable: Boolean

    /** До сервера не дотянулись: нет сети, DNS, таймаут, обрыв соединения. */
    data object Network : AppError {
        override val isRetryable = true
    }

    /**
     * Сервер ответил, но ошибкой. [code] — код бэкенда (`errorCode`) или HTTP;
     * текст ответа в UI не идёт, он остаётся в причине исключения для отчётов.
     */
    data class Api(
        val code: Int? = null,
    ) : AppError {
        override val isRetryable = true
    }

    /** Вход и аккаунт: неверный пароль, занятая почта, нужна повторная авторизация. */
    data class Auth(
        val reason: AuthErrorReason,
    ) : AppError {
        override val isRetryable = false
    }

    /** Локальная база не прочиталась или не записалась. Повтор тут не лечит. */
    data object Database : AppError {
        override val isRetryable = false
    }

    /** Поток котировок оборвался или прислал то, что не разобралось. */
    data object Stream : AppError {
        override val isRetryable = true
    }

    /** То, что ввёл пользователь, не годится. */
    data class Validation(
        val reason: ValidationErrorReason,
    ) : AppError {
        override val isRetryable = false
    }

    /** Всё, что не опознали. Повторить не вредно: чаще всего это что-то временное. */
    data object Unknown : AppError {
        override val isRetryable = true
    }
}
