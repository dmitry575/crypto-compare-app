package com.cryptocompare.helpers.util

/**
 * Словарь названий сетей.
 *
 * Бэкенд отдаёт сеть символа строкой через запятую в том виде, в каком её прислал
 * источник, и форматы у источников разные. Одна и та же сеть на 2026-09-15
 * встречалась как `eth`, `ETH`, `Ethereum`, `ETHEREUM`, `ERC20` и `ETH-ERC20`;
 * Arbitrum — как `arbitrum`, `ARB`, `ARBI`, `ARBEVM`, `Arbitrum One` и
 * `ArbitrumOne`. Всего 225 разных записей на 810 символах.
 *
 * Ключ — запись в нижнем регистре без пробелов, дефисов, подчёркиваний, точек и
 * скобок (см. `networkKey`), значение — имя для экрана. Сети, которых здесь нет,
 * показываются как пришли, с поправкой регистра.
 */
object NetworkConstants {
    val ALIASES: Map<String, String> =
        mapOf(
            "eth" to "Ethereum",
            "ethereum" to "Ethereum",
            "erc20" to "Ethereum",
            "ethereumc" to "Ethereum Classic",
            "ethereumclassic" to "Ethereum Classic",
            "etc" to "Ethereum Classic",
            "bsc" to "BNB Chain",
            "bep20" to "BNB Chain",
            "bnb" to "BNB Chain",
            "bnbsmartchain" to "BNB Chain",
            "bnbsmartchainbsc" to "BNB Chain",
            "opbnb" to "opBNB",
            "sol" to "Solana",
            "solana" to "Solana",
            "spl" to "Solana",
            "trx" to "Tron",
            "tron" to "Tron",
            "trc20" to "Tron",
            "btc" to "Bitcoin",
            "bitcoin" to "Bitcoin",
            "brc20" to "BRC-20",
            "lightning" to "Lightning",
            "arb" to "Arbitrum",
            "arbi" to "Arbitrum",
            "arbevm" to "Arbitrum",
            "arbitrum" to "Arbitrum",
            "arbitrumone" to "Arbitrum",
            "arbnova" to "Arbitrum Nova",
            "arbinova" to "Arbitrum Nova",
            "base" to "Base",
            "baseevm" to "Base",
            "op" to "Optimism",
            "opeth" to "Optimism",
            "optimism" to "Optimism",
            "optimismv2" to "Optimism",
            "matic" to "Polygon",
            "matice" to "Polygon",
            "pol" to "Polygon",
            "polygon" to "Polygon",
            "polygonpos" to "Polygon",
            "avax" to "Avalanche",
            "avaxc" to "Avalanche",
            "avaxcchain" to "Avalanche",
            "cavax" to "Avalanche",
            "ton" to "TON",
            "ton2" to "TON",
            "theopennetworkton" to "TON",
            "sui" to "Sui",
            "suinew" to "Sui",
            "apt" to "Aptos",
            "aptos" to "Aptos",
            "near" to "NEAR",
            "ada" to "Cardano",
            "cardano" to "Cardano",
            "xrp" to "XRP Ledger",
            "ripple" to "XRP Ledger",
            "xrpledger" to "XRP Ledger",
            "doge" to "Dogecoin",
            "dogecoin" to "Dogecoin",
            "ltc" to "Litecoin",
            "litecoin" to "Litecoin",
            "xlm" to "Stellar",
            "stellar" to "Stellar",
            "stellarlumens" to "Stellar",
            "stark" to "Starknet",
            "starknet" to "Starknet",
            "zksync" to "zkSync",
            "zksync2" to "zkSync",
            "zksyncera" to "zkSync",
            "zksera" to "zkSync",
            "zkv2" to "zkSync",
            "zklink" to "zkLink",
            "linea" to "Linea",
            "mantle" to "Mantle",
            "sonic" to "Sonic",
            "monad" to "Monad",
            "mon" to "Monad",
            "sei" to "Sei",
            "seievm" to "Sei",
            "hbar" to "Hedera",
            "hedera" to "Hedera",
            "statemint" to "Polkadot Asset Hub",
            "dotassethub" to "Polkadot Asset Hub",
            "dotsm" to "Polkadot Asset Hub",
            "assethubpolkadot" to "Polkadot Asset Hub",
            "kcc" to "KCC",
            "kaia" to "Kaia",
            "klay" to "Kaia",
            "fil" to "Filecoin",
            "fevm" to "Filecoin",
            "filecoin" to "Filecoin",
            "xlayer" to "X Layer",
            "xlayerweth" to "X Layer",
            "oktc" to "OKTC",
            "xdc" to "XDC",
            "algo" to "Algorand",
            "inj" to "Injective",
            "chz" to "Chiliz",
            "enj" to "Enjin",
            "enjin" to "Enjin",
            "cfx" to "Conflux",
            "cfxcore" to "Conflux",
            "neo3" to "Neo",
            "neon3" to "Neo",
            "n3" to "Neo",
            "plasma" to "Plasma",
            "xpl" to "Plasma",
            "bera" to "Berachain",
            "celo" to "Celo",
            "wld" to "World Chain",
            "robinhood" to "Robinhood Chain",
            "robinhoodchain" to "Robinhood Chain",
            "unichain" to "Unichain",
            "kavaevm" to "Kava",
            "hyperevm" to "HyperEVM",
            "movement" to "Movement",
            "move" to "Movement",
            "flux" to "Flux",
            "zel" to "Flux",
            "ela" to "Elastos",
            "elaevm" to "Elastos",
            "esc" to "Elastos",
            "iotamainnet" to "IOTA",
            "iotex" to "IoTeX",
            "wax" to "WAX",
            "waxp" to "WAX",
            "rune" to "THORChain",
            "lunc" to "Terra Classic",
            "vsys" to "V Systems",
            "zig" to "ZIGChain",
            "zigchain" to "ZIGChain",
            "merlinnetwork" to "Merlin",
            "abcore" to "AB Core",
            "megaeth" to "MegaETH",
            "0g" to "0G",
            // короткие имена, которые правило регистра превратило бы в аббревиатуры
            "hemi" to "Hemi",
            "ink" to "Ink",
            "swan" to "Swan",
            "lisk" to "Lisk",
            "boba" to "Boba",
            "iost" to "IOST",
        )

    /**
     * Эти записи означают «сеть не указана» и в метку не идут: на 2026-09-15
     * источники присылали и `None`, и `NONE`.
     */
    val EMPTY_VALUES: Set<String> = setOf("none", "null", "")

    /**
     * Порядок сетей в метке. Сначала те, по которым пользователь узнаёт монету;
     * остальные идут следом по алфавиту. Без порядка метка одного и того же
     * символа начиналась бы с той сети, которую источник записал первой.
     */
    val PRIORITY: List<String> =
        listOf(
            "Ethereum",
            "BNB Chain",
            "Solana",
            "Tron",
            "Bitcoin",
            "Arbitrum",
            "Base",
            "Optimism",
            "Polygon",
            "Avalanche",
            "TON",
            "Sui",
            "Aptos",
        )

    /** Сколько сетей назвать в короткой метке, прежде чем свернуть хвост в «+N». */
    const val LABEL_MAX_NAMES = 2

    const val SEPARATOR = ","
    const val LABEL_JOINER = ", "
    const val LABEL_MORE_FORMAT = "%1\$s +%2\$d"

    /** Короче — аббревиатура (`XDC`, `KCC`), длиннее — имя (`Superseed`). */
    const val ABBREVIATION_MAX_LENGTH = 4

    /** Префикс базового актива у okx: `ETH-Arbitrum One`, у OKTC — `ETHK-OKTC`. */
    const val WRAPPED_PREFIX_SUFFIX = "k"
    const val PREFIX_DELIMITER = '-'
}
