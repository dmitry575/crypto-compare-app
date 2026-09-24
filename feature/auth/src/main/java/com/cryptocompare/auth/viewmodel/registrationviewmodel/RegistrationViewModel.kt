package com.cryptocompare.auth.viewmodel.registrationviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.auth.util.withoutValidation
import com.cryptocompare.domain.usecase.auth.IsValidEmailUseCase
import com.cryptocompare.domain.usecase.auth.SignInWithGoogleUseCase
import com.cryptocompare.domain.usecase.auth.SignUpWithEmailUseCase
import com.cryptocompare.helpers.util.PasswordConstants
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AuthErrorReason
import com.cryptocompare.model.error.ValidationErrorReason
import com.cryptocompare.model.error.asAppError
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
            _uiState.update { uiState ->
                uiState.copy(
                    email = email,
                    error = uiState.error.withoutValidation(ValidationErrorReason.INVALID_EMAIL),
                )
            }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { uiState ->
                uiState.copy(
                    password = password,
                    passwordLengthMet = password.length >= PasswordConstants.MIN_LENGTH,
                    passwordLetterMet = password.any { it.isLetter() },
                    passwordNumberMet = password.any { it.isDigit() },
                    // несовпадение чинится правкой любого из двух полей
                    error =
                        uiState.error.withoutValidation(
                            ValidationErrorReason.PASSWORD_TOO_WEAK,
                            ValidationErrorReason.PASSWORDS_DO_NOT_MATCH,
                        ),
                )
            }
        }

        fun onConfirmPasswordChange(confirmPassword: String) {
            _uiState.update { uiState ->
                uiState.copy(
                    confirmPassword = confirmPassword,
                    error = uiState.error.withoutValidation(ValidationErrorReason.PASSWORDS_DO_NOT_MATCH),
                )
            }
        }

        fun signUpWithEmail() {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password
            val confirmPassword = _uiState.value.confirmPassword

            if (!isValidEmailUseCase(email)) {
                _uiState.update { uiState ->
                    uiState.copy(error = AppError.Validation(ValidationErrorReason.INVALID_EMAIL))
                }
                return
            }

            if (!_uiState.value.passwordLengthMet ||
                !_uiState.value.passwordLetterMet ||
                !_uiState.value.passwordNumberMet
            ) {
                _uiState.update { it.copy(error = AppError.Validation(ValidationErrorReason.PASSWORD_TOO_WEAK)) }
                return
            }
            if (password != confirmPassword) {
                _uiState.update { it.copy(error = AppError.Validation(ValidationErrorReason.PASSWORDS_DO_NOT_MATCH)) }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, error = null) }

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
                                error = error.asAppError(),
                            )
                        }
                    }
            }
        }

        fun signUpWithGoogle(idToken: String) {
            if (idToken.isBlank()) {
                _uiState.update { uiState -> uiState.copy(error = AppError.Auth(AuthErrorReason.GOOGLE_TOKEN_MISSING)) }
                return
            }

            _uiState.update { uiState -> uiState.copy(isLoading = true, error = null) }

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
                                error = error.asAppError(),
                            )
                        }
                    }
            }
        }

        fun onGoogleError(error: AppError) {
            _uiState.update { it.copy(error = error) }
        }
    }
