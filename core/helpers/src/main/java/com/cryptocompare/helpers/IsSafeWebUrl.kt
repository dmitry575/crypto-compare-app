package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.UrlConstants
import java.net.URI

/**
 * Можно ли открывать эту ссылку во внешнем браузере.
 *
 * Ссылка приходит с бэкенда, то есть управляется не нами. Отдать её в
 * `ACTION_VIEW` как есть нельзя: `intent://` умеет адресовать чужие компоненты,
 * `javascript:` и `data:` исполняются в браузере, `file://` и `content://`
 * указывают внутрь устройства. Пропускаем только http и https с непустым хостом.
 *
 * Схему не дописываем: голый хост от бэкенда — это баг, и он должен быть виден
 * как отсутствующая кнопка, а не превращаться нами в навигацию.
 *
 * Разбор через [URI], а не `android.net.Uri`: тот на JVM возвращает заглушки,
 * и проверку было бы не покрыть обычным юнит-тестом.
 */
fun String?.isSafeWebUrl(): Boolean {
    val value = this?.trim().orEmpty()
    if (value.isEmpty()) return false

    return runCatching {
        val uri = URI(value)
        val scheme = uri.scheme?.lowercase()

        scheme != null && scheme in UrlConstants.WEB_SCHEMES && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
