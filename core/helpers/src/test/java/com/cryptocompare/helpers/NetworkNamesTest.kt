package com.cryptocompare.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Строки `network` взяты с живого бэкенда 2026-09-15. */
class NetworkNamesTest {
    @Test
    fun `lowercase codes of one source become names`() {
        assertEquals(
            listOf("Ethereum", "BNB Chain", "Arbitrum", "Base", "Optimism", "KCC", "Robinhood Chain"),
            networkNames("arbitrum,base,bsc,eth,kcc,optimism,robinhood", baseAsset = "eth"),
        )
    }

    @Test
    fun `uppercase codes of another source land on the same names`() {
        val names = networkNames("ABSTRACT,ARB,ARBNOVA,BASE,BSC,ETH,HEMI,OP,SOL,STARK", baseAsset = "eth")

        assertEquals(listOf("Ethereum", "BNB Chain", "Solana", "Arbitrum", "Base", "Optimism"), names.take(6))
        assertTrue(names.containsAll(listOf("Arbitrum Nova", "Starknet", "Abstract", "Hemi")))
    }

    @Test
    fun `full chain names are recognised`() {
        assertEquals(
            listOf(
                "Ethereum",
                "BNB Chain",
                "Arbitrum",
                "Base",
                "Robinhood Chain",
                "XT Smart Chain",
                "zkLink",
                "zkSync",
            ),
            networkNames(
                "Arbitrum One,BNB Smart Chain,Base,Ethereum,ROBINHOOD,XT Smart Chain,ZKLINK,ZkSync Era",
                baseAsset = "eth",
            ),
        )
    }

    @Test
    fun `okx prefixes with the base asset are stripped`() {
        assertEquals(
            listOf("Ethereum", "Arbitrum", "Base", "Optimism", "Linea", "OKTC", "X Layer"),
            networkNames(
                "ETH-Arbitrum One,ETH-Base,ETH-ERC20,ETH-Linea,ETH-Optimism,ETH-X Layer,ETH-X Layer (WETH),ETHK-OKTC",
                baseAsset = "eth",
            ),
        )
        assertEquals(listOf("Ethereum"), networkNames("AAVE-ERC20", baseAsset = "aave"))
        assertEquals(listOf("Base"), networkNames("CP-Base", baseAsset = "cp"))
    }

    @Test
    fun `a hyphen that belongs to the name is not a prefix`() {
        // AVAX-C у пары AVAX/USDT: срежь «AVAX-» — останется «C»
        assertEquals(listOf("Avalanche"), networkNames("AVAX,AVAX-C", baseAsset = "avax"))
        assertEquals(listOf("Avalanche"), networkNames("AVAXC-Chain", baseAsset = "usdc"))
    }

    @Test
    fun `token standards map to their chains`() {
        assertEquals(listOf("Ethereum", "BNB Chain", "Solana"), networkNames("SOL,BEP20,ERC20", baseAsset = "usdc"))
        assertEquals(listOf("Tron"), networkNames("TRC20", baseAsset = "usdt"))
    }

    @Test
    fun `ethereum classic is not ethereum`() {
        assertEquals(listOf("Ethereum Classic"), networkNames("ETHEREUMC", baseAsset = "etc"))
        assertEquals(listOf("Ethereum"), networkNames("ETHEREUM", baseAsset = "eth"))
    }

    @Test
    fun `duplicates collapse after normalisation`() {
        assertEquals(listOf("TON"), networkNames("ton,ton2", baseAsset = "cati"))
        assertEquals(listOf("BNB Chain"), networkNames("bnb,bsc", baseAsset = "bnb"))
    }

    @Test
    fun `none and blanks mean no network`() {
        assertTrue(networkNames(null, baseAsset = "bel").isEmpty())
        assertTrue(networkNames("", baseAsset = "bel").isEmpty())
        assertTrue(networkNames("None", baseAsset = "bel").isEmpty())
        assertEquals(listOf("Ethereum"), networkNames("NONE,ETH", baseAsset = "bel"))
    }

    @Test
    fun `unknown networks keep their name with sane casing`() {
        assertEquals(listOf("Superseed", "XDC"), networkNames("SUPERSEED,xdc", baseAsset = "x"))
        assertEquals(listOf("Codex"), networkNames("CODEX", baseAsset = "x"))
        assertEquals(listOf("MegaETH"), networkNames("MegaETH", baseAsset = "x"))
    }

    @Test
    fun `short label names two networks and counts the rest`() {
        val names = networkNames("arbitrum,base,bsc,eth,kcc,optimism,robinhood", baseAsset = "eth")

        assertEquals("Ethereum, BNB Chain +5", names.toNetworkLabel())
    }

    @Test
    fun `short label without a tail when everything fits`() {
        assertEquals("Solana", listOf("Solana").toNetworkLabel())
        assertEquals("Ethereum, BNB Chain", listOf("Ethereum", "BNB Chain").toNetworkLabel())
    }

    @Test
    fun `no networks means no label`() {
        assertNull(emptyList<String>().toNetworkLabel())
    }
}
