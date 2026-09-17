package com.cryptocompare.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cryptocompare.app.worker.refresh.RefreshCatalogWorker
import com.cryptocompare.app.worker.sync.SyncFavouritesWorker
import com.cryptocompare.helpers.util.WorkerConstants
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleDailyRefreshCatalog(context: Context) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val refreshRequest =
            PeriodicWorkRequestBuilder<RefreshCatalogWorker>(20, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkerConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            refreshRequest,
        )
    }

    fun scheduleFavouritesSync(context: Context) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val syncRequest =
            PeriodicWorkRequestBuilder<SyncFavouritesWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        // UPDATE, а не KEEP: имя класса воркера лежит в базе WorkManager, и у тех,
        // кто уже обновился с прошлой версии, там осталось старое. KEEP сохранил бы
        // запись на класс, которого больше нет, и синхронизация молча перестала бы
        // запускаться
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkerConstants.UNIQUE_FAVOURITES_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest,
        )
    }
}
