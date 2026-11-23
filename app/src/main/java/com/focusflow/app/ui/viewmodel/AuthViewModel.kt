package com.focusflow.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<Boolean> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.getCurrentUserId() != null
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.login(email, password).collect { result ->
                _isLoading.value = false
                result.onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Login failed"
                }
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.register(email, password).collect { result ->
                _isLoading.value = false
                result.onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Registration failed"
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
