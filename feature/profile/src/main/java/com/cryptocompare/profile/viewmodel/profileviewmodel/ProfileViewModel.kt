package com.cryptocompare.profile.viewmodel.profileviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptocompare.domain.usecase.auth.GetCurrentUserUseCase
import com.cryptocompare.domain.usecase.profile.DeleteAccountUseCase
import com.cryptocompare.domain.usecase.profile.SignOutUseCase
import com.cryptocompare.helpers.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
        private val signOutUseCase: SignOutUseCase,
        private val deleteAccountUseCase: DeleteAccountUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState = _uiState.asStateFlow()

        init {
            loadUser()
        }

        fun onSignOutClick() {
            _uiState.update { uiState -> uiState.copy(showSignOutConfirmation = true) }
        }

        fun onSignOutDismissed() {
            _uiState.update { uiState -> uiState.copy(showSignOutConfirmation = false) }
        }

        fun onSignOutConfirmed() {
            viewModelScope.launch {
                _uiState.update { uiState ->
                    uiState.copy(showSignOutConfirmation = false, isLoading = true, errorMessage = null)
                }
                runCatching { signOutUseCase() }
                    .onSuccess { clearSession() }
                    .onFailure(::showError)
            }
        }

        fun onDeleteAccountClick() {
            _uiState.update { uiState -> uiState.copy(showDeleteConfirmation = true) }
        }

        fun onDeleteAccountDismissed() {
            _uiState.update { uiState -> uiState.copy(showDeleteConfirmation = false) }
        }

        fun onDeleteAccountConfirmed() {
            viewModelScope.launch {
                _uiState.update { uiState ->
                    uiState.copy(showDeleteConfirmation = false, isLoading = true, errorMessage = null)
                }
                deleteAccountUseCase()
                    .onSuccess { clearSession() }
                    .onFailure(::showError)
            }
        }

        fun onErrorShown() {
            _uiState.update { uiState -> uiState.copy(errorMessage = null) }
        }

        private fun loadUser() {
            val user = runCatching { getCurrentUserUseCase() }.getOrNull()
            _uiState.update { uiState -> uiState.copy(user = user) }
        }

        private fun clearSession() {
            _uiState.update { uiState ->
                uiState.copy(user = null, isLoading = false, isSignedOut = true)
            }
        }

        private fun showError(error: Throwable) {
            _uiState.update { uiState ->
                uiState.copy(isLoading = false, errorMessage = error.toUserMessage())
            }
        }
    }
