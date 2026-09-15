package com.cryptocompare.app.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cryptocompare.domain.usecase.pairs.StreamPauseUseCase
import com.cryptocompare.domain.usecase.pairs.StreamResumeUseCase
import javax.inject.Inject

/**
 * Держит поток котировок открытым только пока приложение на экране.
 *
 * Вешается на `ProcessLifecycleOwner`, а не на активность: тот сообщает об уходе
 * в фон с задержкой, поэтому поворот экрана и переход между активностями
 * соединение не рвут.
 *
 * Раньше поток жил, пока жив процесс. Закрывал его только `MainViewModel` в
 * onCleared, а это выход из каталога, а не сворачивание приложения.
 */
class TickerStreamLifecycleObserver
    @Inject
    constructor(
        private val streamPauseUseCase: StreamPauseUseCase,
        private val streamResumeUseCase: StreamResumeUseCase,
    ) : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            streamResumeUseCase()
        }

        override fun onStop(owner: LifecycleOwner) {
            streamPauseUseCase()
        }
    }
