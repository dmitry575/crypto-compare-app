package com.cryptocompare.data.transactionrunner

import androidx.room.withTransaction
import com.cryptocompare.data.local.CryptoCompareDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseTransactionRunnerImpl
    @Inject
    constructor(
        private val database: CryptoCompareDatabase,
    ) : DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
    }
