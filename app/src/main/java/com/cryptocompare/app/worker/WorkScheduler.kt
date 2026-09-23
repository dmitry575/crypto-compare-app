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
    /**
     * Фоновая перекачка каталога. Раньше называлась `scheduleDailyRefreshCatalog`,
     * хотя интервал давно 20 минут — см. [WorkerConstants.CATALOG_REFRESH_INTERVAL_MINUTES].
     *
     * `UPDATE`, а не `KEEP`: при `KEEP` новый интервал к уже стоящей работе не
     * применялся, и у тех, кто поставил приложение до смены интервала, каталог
     * так и перекачивался раз в сутки — даже после обновления. `UPDATE` правит
     * стоящую работу на месте: расписание не сбрасывается, идущий запуск не
     * прерывается, а следующая смена интервала дойдёт до всех сама.
     */
    fun scheduleCatalogRefresh(context: Context) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val refreshRequest =
            PeriodicWorkRequestBuilder<RefreshCatalogWorker>(
                WorkerConstants.CATALOG_REFRESH_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WorkerConstants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            refreshRequest,
        )
    }

    fun scheduleFavouritesSync(context: Context) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val syncRequest =
            PeriodicWorkRequestBuilder<SyncFavouritesWorker>(
                WorkerConstants.FAVOURITES_SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).setConstraints(constraints)
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
