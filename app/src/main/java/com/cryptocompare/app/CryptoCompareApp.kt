package com.cryptocompare.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.cryptocompare.app.lifecycle.TickerStreamLifecycleObserver
import com.cryptocompare.app.worker.WorkScheduler
import com.cryptocompare.domain.usecase.pairs.SyncLiveBestPricesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CryptoCompareApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var tickerStreamLifecycleObserver: TickerStreamLifecycleObserver

    @Inject
    lateinit var syncLiveBestPricesUseCase: SyncLiveBestPricesUseCase

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(tickerStreamLifecycleObserver)
        // лучшие пары из сокета пишет в каталог один писатель на весь процесс, а не
        // каждый экран своей пачкой; scope процесса не отменяется до его смерти
        ProcessLifecycleOwner.get().lifecycleScope.launch { syncLiveBestPricesUseCase() }
        WorkScheduler.scheduleCatalogRefresh(this)
        WorkScheduler.scheduleFavouritesSync(this)
    }
}
