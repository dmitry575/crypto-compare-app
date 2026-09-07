package com.cryptocompare.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Открыть ссылку во внешнем браузере. Возвращает `false`, если открыть не вышло.
 *
 * Проверять доступность заранее через `resolveActivity()` нельзя: на Android 11+
 * видимость пакетов скроет браузер от опроса без блока `<queries>`, и метод
 * вернёт null на исправном устройстве. Сам запуск `ACTION_VIEW` для http/https
 * фильтрацией не затронут, поэтому правильный порядок — пробовать и ловить.
 */
fun Context.openExternalUrl(url: String?): Boolean {
    if (!url.isSafeWebUrl()) return false

    return runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url!!.trim().toUri()))
        true
    }.getOrElse { error ->
        if (error is ActivityNotFoundException) false else throw error
    }
}
