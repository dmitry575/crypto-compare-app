package com.cryptocompare.auth.viewmodel.registrationviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithGoogleUseCase
import com.cryptocompare.domain.usecase.auth.SignUpWithEmailUseCase
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.helpers.util.PasswordConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel
    @Inject
    constructor(
        private val isValidEmailUseCase: IsValidEmailUseCase,
        private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
        private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RegistrationUiState())
        val uiState = _uiState.asStateFlow()

        fun onEmailChange(email: String) {
            _uiState.update { uiState -> uiState.copy(email = email) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { uiState ->
                uiState.copy(
                    password = password,
                    passwordLengthMet = password.length >= PasswordConstants.MIN_LENGTH,
                    passwordLetterMet = password.any { it.isLetter() },
                    passwordNumberMet = password.any { it.isDigit() },
                )
            }
        }

        fun onConfirmPasswordChange(confirmPassword: String) {
            _uiState.update { uiState -> uiState.copy(confirmPassword = confirmPassword) }
        }

        fun signUpWithEmail() {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password
            val confirmPassword = _uiState.value.confirmPassword

            if (!isValidEmailUseCase(email)) {
                _uiState.update { uiState -> uiState.copy(errorMessage = "Incorrect email was entered") }
                return
            }

            if (!_uiState.value.passwordLengthMet ||
                !_uiState.value.passwordLetterMet ||
                !_uiState.value.passwordNumberMet
            ) {
                _uiState.update { it.copy(errorMessage = "Password must be stronger") }
                return
            }
            if (password != confirmPassword) {
                _uiState.update { it.copy(errorMessage = "Passwords don't match") }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, errorMessage = null) }

            viewModelScope.launch {
                signUpWithEmailUseCase(email, password)
                    .onSuccess {
                        _uiState.update { uiState ->
                            uiState.copy(isLoading = false, isAuthenticated = true)
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

        fun signUpWithGoogle(idToken: String) {
            if (idToken.isBlank()) {
                _uiState.update { uiState -> uiState.copy(errorMessage = "Google token not found") }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, errorMessage = null) }

            viewModelScope.launch {
                signInWithGoogleUseCase(idToken)
                    .onSuccess {
                        _uiState.update { uiState ->
                            uiState.copy(isLoading = false, isAuthenticated = true)
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

        fun onGoogleError(message: String) {
            _uiState.update { it.copy(errorMessage = message) }
        }
    }
