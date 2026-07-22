package com.cryptocompare.auth.viewmodel.loginviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithGoogleUseCase
import com.cryptocompare.helpers.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val signInWithEmailUseCase: SignInWithEmailUseCase,
        private val isValidEmailUseCase: IsValidEmailUseCase,
        private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState = _uiState.asStateFlow()

        fun onEmailChange(email: String) {
            _uiState.update { uiState -> uiState.copy(email = email) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { uiState -> uiState.copy(password = password) }
        }

        fun signInWithEmail() {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            if (!isValidEmailUseCase(email)) {
                _uiState.update { uiState -> uiState.copy(errorMessage = "Incorrect email was entered") }
                return
            }

            if (password.length < 6) {
                _uiState.update { it.copy(errorMessage = "Password must have more than 6 symbols") }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, errorMessage = null) }

            viewModelScope.launch {
                signInWithEmailUseCase(email, password)
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

        fun signInWithGoogle(idToken: String) {
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
