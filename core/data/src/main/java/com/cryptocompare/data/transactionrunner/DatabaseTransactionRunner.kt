package com.cryptocompare.data.transactionrunner

interface DatabaseTransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
