package com.cryptocompare.app.worker.refresh

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptocompare.domain.usecase.pairs.RefreshCatalogUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RefreshCatalogWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted appParams: WorkerParameters,
        private val refreshCatalogUseCase: RefreshCatalogUseCase,
    ) : CoroutineWorker(context, appParams) {
        override suspend fun doWork(): Result =
            refreshCatalogUseCase().fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
