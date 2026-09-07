package com.cryptocompare.model.provider

data class Provider(
    val id: Int,
    val name: String?,
    /**
     * Реферальная ссылка на биржу. Раньше поле называлось webSite и означало
     * сайт биржи; теперь бэкенд кладёт сюда партнёрскую ссылку.
     *
     * Приходит снаружи, поэтому перед открытием проверяется `isSafeWebUrl()`.
     */
    val referralUrl: String?,
    val status: ProviderStatus,
)
