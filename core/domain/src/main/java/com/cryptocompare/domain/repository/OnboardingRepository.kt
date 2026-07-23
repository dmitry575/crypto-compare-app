package com.cryptocompare.domain.repository

/**
 * Показывали ли онбординг. Флаг привязан к устройству, а не к аккаунту:
 * онбординг рассказывает про продукт, а не про авторизацию, и при смене
 * аккаунта повторяться не должен.
 */
interface OnboardingRepository {
    /** Разовое чтение: сплеш решает маршрут один раз и на поток не подписан. */
    suspend fun hasSeenOnboarding(): Boolean

    suspend fun markOnboardingSeen()
}
