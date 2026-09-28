package com.example.myapplication.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val usernameText: String = "",
    val passwordText: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(usernameText = username, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(passwordText = password, errorMessage = null)
    }

    fun login(onSuccess: (String) -> Unit) {
        val username = _uiState.value.usernameText.trim()
        val password = _uiState.value.passwordText.trim()

        if (username.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Username and password cannot be empty.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when {
                username.equals("admin", ignoreCase = true) && password == "admin123" -> {
                    userPreferencesRepository.saveUserSession(role = "ADMIN", username = username)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess("ADMIN")
                }
                username.equals("staff", ignoreCase = true) && password == "staff123" -> {
                    userPreferencesRepository.saveUserSession(role = "STAFF", username = username)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess("STAFF")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid credentials. Hint: admin/admin123 or staff/staff123"
                    )
                }
            }
        }
    }
}
