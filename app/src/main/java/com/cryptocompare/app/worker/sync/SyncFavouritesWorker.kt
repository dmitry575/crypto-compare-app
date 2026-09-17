package com.cryptocompare.app.worker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptocompare.domain.usecase.pairs.SyncFavouriteSymbolsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncFavouritesWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted appParams: WorkerParameters,
        private val syncFavouriteSymbolsUseCase: SyncFavouriteSymbolsUseCase,
    ) : CoroutineWorker(context, appParams) {
        override suspend fun doWork(): Result =
            syncFavouriteSymbolsUseCase().fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
