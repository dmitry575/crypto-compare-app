package com.cryptocompare.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cryptocompare.helpers.util.WorkerConstants
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleDailyRefreshCatalog(context: Context) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val refreshRequest =
            PeriodicWorkRequestBuilder<RefreshCatalogWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkerConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            refreshRequest,
        )
    }
}
