package com.cryptocompare.profile.viewmodel.changepasswordviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.profile.ChangePasswordUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.helpers.util.PasswordConstants
import com.cryptocompare.profile.util.ChangePasswordError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel
    @Inject
    constructor(
        private val changePasswordUseCase: ChangePasswordUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChangePasswordUiState())
        val uiState = _uiState.asStateFlow()

        fun onCurrentPasswordChange(currentPassword: String) {
            _uiState.update { uiState -> uiState.copy(currentPassword = currentPassword) }
        }

        fun onNewPasswordChange(newPassword: String) {
            _uiState.update { uiState ->
                uiState.copy(
                    newPassword = newPassword,
                    passwordLengthMet = newPassword.length >= PasswordConstants.MIN_LENGTH,
                    passwordLetterMet = newPassword.any { it.isLetter() },
                    passwordNumberMet = newPassword.any { it.isDigit() },
                )
            }
        }

        fun onConfirmPasswordChange(confirmPassword: String) {
            _uiState.update { uiState -> uiState.copy(confirmPassword = confirmPassword) }
        }

        fun changePassword() {
            val state = _uiState.value
            val validationError = state.validate()
            if (validationError != null) {
                _uiState.update { uiState -> uiState.copy(validationError = validationError) }
                return
            }

            _uiState.update { uiState ->
                uiState.copy(isLoading = true, validationError = null, errorMessage = null)
            }

            viewModelScope.launch {
                changePasswordUseCase(state.currentPassword, state.newPassword)
                    .onSuccess {
                        _uiState.update { uiState ->
                            uiState.copy(isLoading = false, isPasswordChanged = true)
                        }
                    }.onFailure { error ->
                        _uiState.update { uiState ->
                            uiState.copy(isLoading = false, errorMessage = error.toUserMessage())
                        }
                    }
            }
        }

        fun onErrorShown() {
            _uiState.update { uiState -> uiState.copy(validationError = null, errorMessage = null) }
        }

        private fun ChangePasswordUiState.validate(): ChangePasswordError? =
            when {
                currentPassword.isBlank() -> ChangePasswordError.CURRENT_PASSWORD_EMPTY
                !passwordLengthMet || !passwordLetterMet || !passwordNumberMet ->
                    ChangePasswordError.NEW_PASSWORD_TOO_WEAK
                newPassword != confirmPassword -> ChangePasswordError.PASSWORDS_DO_NOT_MATCH
                newPassword == currentPassword -> ChangePasswordError.NEW_PASSWORD_SAME_AS_CURRENT
                else -> null
            }
    }
