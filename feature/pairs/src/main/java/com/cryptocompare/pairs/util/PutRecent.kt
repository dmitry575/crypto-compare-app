package com.cryptocompare.pairs.util

/**
 * Кладёт значение как самое свежее и вытесняет давно не тронутые, пока ключей
 * больше [maxSize].
 *
 * Опирается на порядок `mutableMapOf()` — это `LinkedHashMap` в порядке вставки,
 * поэтому «удалить и положить заново» переносит ключ в конец, а первый ключ —
 * самый давний. Своего класса-кеша ради этого не нужно.
 */
internal fun <K, V> MutableMap<K, V>.putRecent(
    key: K,
    value: V,
    maxSize: Int,
) {
    remove(key)
    put(key, value)
    while (size > maxSize) {
        remove(keys.first())
    }
}
