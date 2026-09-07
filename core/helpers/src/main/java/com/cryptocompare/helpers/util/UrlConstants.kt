package com.cryptocompare.helpers.util

/** Правила обращения с внешними ссылками. */
object UrlConstants {
    /**
     * Схемы, которые нам можно открывать наружу.
     *
     * Белый список, а не чёрный: ссылки приходят с бэкенда, и перечислить всё
     * нежелательное (`intent://`, `javascript:`, `file://`, `content://`, `data:`)
     * заведомо не выйдет — схем больше, чем мы можем предугадать.
     */
    val WEB_SCHEMES = setOf("http", "https")
}
