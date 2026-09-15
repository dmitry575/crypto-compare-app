package com.cryptocompare.helpers

import com.cryptocompare.helpers.util.NetworkConstants
import java.util.Locale

/**
 * Сети символа в едином виде: без повторов, крупные сначала.
 *
 * Бэкенд делит одну пару на несколько символов по сетям: USDC в Ethereum и USDC
 * в Solana — разные активы, и сравнивать их цены значит получать спреды в тысячи
 * процентов. Пользователю эти символы надо различать, а строка `network`
 * приходит в формате того источника, откуда её взял бэкенд, — подробности в
 * [NetworkConstants].
 *
 * [baseAsset] нужен для формата okx, где перед сетью стоит базовый актив:
 * `ETH-Arbitrum One`, `AAVE-ERC20`, `ETHK-OKTC`. Срезается только он — у
 * `AVAXC-Chain` дефис часть имени, и такие записи сначала ищутся в словаре целиком.
 */
fun networkNames(
    raw: String?,
    baseAsset: String?,
): List<String> {
    if (raw.isNullOrBlank()) return emptyList()

    val base = baseAsset?.trim()?.lowercase(Locale.ROOT).orEmpty()

    val names =
        raw
            .split(NetworkConstants.SEPARATOR)
            .map { it.trim() }
            .filter { it.lowercase(Locale.ROOT) !in NetworkConstants.EMPTY_VALUES }
            .map { token -> canonicalName(token, base) }
            .distinct()

    val priority = NetworkConstants.PRIORITY
    return names.sortedWith(
        compareBy<String> { name -> priority.indexOf(name).takeIf { it >= 0 } ?: priority.size }
            .thenBy { it.lowercase(Locale.ROOT) },
    )
}

/**
 * Короткая метка: «Ethereum, Arbitrum +5». `null`, если сети неизвестны —
 * пустая метка ничего бы не различала.
 */
fun List<String>.toNetworkLabel(maxNames: Int = NetworkConstants.LABEL_MAX_NAMES): String? {
    if (isEmpty()) return null

    val shown = take(maxNames).joinToString(NetworkConstants.LABEL_JOINER)
    val rest = size - maxNames

    return if (rest > 0) String.format(Locale.ROOT, NetworkConstants.LABEL_MORE_FORMAT, shown, rest) else shown
}

private fun canonicalName(
    token: String,
    base: String,
): String {
    NetworkConstants.ALIASES[networkKey(token)]?.let { return it }

    val withoutBase = token.withoutBasePrefix(base)
    if (withoutBase != token) {
        NetworkConstants.ALIASES[networkKey(withoutBase)]?.let { return it }
    }

    return withoutBase.displayCase()
}

private fun networkKey(token: String): String = token.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

private fun String.withoutBasePrefix(base: String): String {
    if (base.isEmpty()) return this

    val delimiter = indexOf(NetworkConstants.PREFIX_DELIMITER)
    if (delimiter <= 0 || delimiter == lastIndex) return this

    val prefix = substring(0, delimiter).lowercase(Locale.ROOT)
    val isBase = prefix == base || prefix == base + NetworkConstants.WRAPPED_PREFIX_SUFFIX

    return if (isBase) substring(delimiter + 1).trim() else this
}

/**
 * Неизвестная сеть как пришла, но без крика и без шёпота: `SUPERSEED` и
 * `superseed` становятся «Superseed», короткие `xdc` — «XDC». Смешанный регистр
 * (`MegaETH`) источник выбрал сам, его не трогаем.
 */
private fun String.displayCase(): String {
    val uniform = this == lowercase(Locale.ROOT) || this == uppercase(Locale.ROOT)
    if (!uniform) return this

    return if (length <= NetworkConstants.ABBREVIATION_MAX_LENGTH) {
        uppercase(Locale.ROOT)
    } else {
        lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
