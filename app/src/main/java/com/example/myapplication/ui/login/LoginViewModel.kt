package com.example.myapplication.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
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

    private val app = application as AttendanceApplication
    private val userPreferencesRepository = app.container.userPreferencesRepository
    private val attendanceRepository = app.container.attendanceRepository

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(usernameText = username, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(passwordText = password, errorMessage = null)
    }

    fun login(onSuccess: (String) -> Unit) {
        val inputUsername = _uiState.value.usernameText.trim()
        val password = _uiState.value.passwordText.trim()

        if (inputUsername.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Username and password cannot be empty.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            // Admin login check
            if (inputUsername.equals("admin", ignoreCase = true) && password == "admin123") {
                userPreferencesRepository.saveUserSession(role = "ADMIN", username = "admin")
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess("ADMIN")
                return@launch
            }

            // Staff login check by Employee ID
            val staffMember = attendanceRepository.getStaffByEmployeeId(inputUsername)
            if (staffMember != null && (password == "staff123")) {
                userPreferencesRepository.saveUserSession(role = "STAFF", username = staffMember.employeeId)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess("STAFF")
                return@launch
            }

            // Fallback for generic "staff" demo account if no staff exists with ID "staff"
            if (inputUsername.equals("staff", ignoreCase = true) && password == "staff123") {
                userPreferencesRepository.saveUserSession(role = "STAFF", username = "staff")
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess("STAFF")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Invalid credentials or Employee ID not found.\nDemo: Admin (admin/admin123) or Staff (<Employee ID>/staff123)."
            )
        }
    }
}
