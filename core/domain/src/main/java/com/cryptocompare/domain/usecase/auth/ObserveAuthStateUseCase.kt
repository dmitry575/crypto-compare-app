package com.cryptocompare.domain.usecase.auth

import com.cryptocompare.domain.repository.AuthRepository
import com.cryptocompare.model.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Кто сейчас в приложении: пользователь или гость (`null`).
 *
 * Поток, а не разовый [GetCurrentUserUseCase]: вход и выход происходят при живых
 * экранах — после выхода профиль должен сам показать приглашение войти, а каталог
 * снова спрашивать вход при попытке добавить в избранное.
 */
class ObserveAuthStateUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke(): Flow<AuthUser?> = authRepository.observeAuthState()
    }
