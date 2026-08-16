package com.cryptocompare.app.worker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptocompare.domain.usecase.pairs.SyncFavouriteTickersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncFavouriteTickersWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted appParams: WorkerParameters,
        private val syncFavouriteTickersUseCase: SyncFavouriteTickersUseCase,
    ) : CoroutineWorker(context, appParams) {
        override suspend fun doWork(): Result =
            syncFavouriteTickersUseCase().fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
