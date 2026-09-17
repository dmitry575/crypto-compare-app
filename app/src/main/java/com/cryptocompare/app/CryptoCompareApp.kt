package com.cryptocompare.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.cryptocompare.app.lifecycle.TickerStreamLifecycleObserver
import com.cryptocompare.app.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CryptoCompareApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var tickerStreamLifecycleObserver: TickerStreamLifecycleObserver

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(tickerStreamLifecycleObserver)
        WorkScheduler.scheduleDailyRefreshCatalog(this)
        WorkScheduler.scheduleFavouritesSync(this)
    }
}
