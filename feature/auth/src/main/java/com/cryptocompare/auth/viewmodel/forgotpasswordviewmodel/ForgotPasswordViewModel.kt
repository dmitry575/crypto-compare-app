package com.cryptocompare.auth.viewmodel.forgotpasswordviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.cryptocompare.helpers.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
        private val isValidEmailUseCase: IsValidEmailUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ForgotPasswordUiState())
        val uiState = _uiState.asStateFlow()

        fun onEmailChange(email: String) {
            _uiState.update { uiState -> uiState.copy(email = email) }
        }

        fun sendResetEmail() {
            val email = _uiState.value.email.trim()

            if (!isValidEmailUseCase(email)) {
                _uiState.update { uiState -> uiState.copy(errorMessage = AuthConstants.Errors.INVALID_EMAIL) }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, errorMessage = null) }

            viewModelScope.launch {
                sendPasswordResetEmailUseCase(email)
                    .onSuccess {
                        _uiState.update { uiState ->
                            uiState.copy(isLoading = false, isEmailSent = true)
                        }
                    }.onFailure { error ->
                        _uiState.update { uiState ->
                            uiState.copy(
                                isLoading = false,
                                errorMessage = error.toUserMessage(),
                            )
                        }
                    }
            }
        }
    }
