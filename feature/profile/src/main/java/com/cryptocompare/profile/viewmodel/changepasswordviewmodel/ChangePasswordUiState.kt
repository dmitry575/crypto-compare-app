package com.cryptocompare.profile.viewmodel.changepasswordviewmodel

import com.cryptocompare.profile.util.ChangePasswordError

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    /** Пароль сменён — экран показывает подтверждение и уходит назад. */
    val isPasswordChanged: Boolean = false,
    /** Наша проверка формы: текст подставляет экран из ресурсов. */
    val validationError: ChangePasswordError? = null,
    /** Текст ошибки от Firebase — переводить нечего, показываем как есть. */
    val errorMessage: String? = null,
    val passwordLengthMet: Boolean = false,
    val passwordLetterMet: Boolean = false,
    val passwordNumberMet: Boolean = false,
)
