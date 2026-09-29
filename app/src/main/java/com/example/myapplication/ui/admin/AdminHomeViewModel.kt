package com.example.myapplication.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
import com.example.myapplication.data.model.Staff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddStaffUiState(
    val showDialog: Boolean = false,
    val nameInput: String = "",
    val employeeIdInput: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false
)

class AdminHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AttendanceApplication
    private val attendanceRepository = app.container.attendanceRepository
    private val userPreferencesRepository = app.container.userPreferencesRepository

    val userName: StateFlow<String?> = userPreferencesRepository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val staffList: StateFlow<List<Staff>> = attendanceRepository.getAllStaff().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _addStaffUiState = MutableStateFlow(AddStaffUiState())
    val addStaffUiState: StateFlow<AddStaffUiState> = _addStaffUiState.asStateFlow()

    fun openAddStaffDialog() {
        _addStaffUiState.value = AddStaffUiState(showDialog = true)
    }

    fun dismissAddStaffDialog() {
        _addStaffUiState.value = _addStaffUiState.value.copy(showDialog = false, errorMessage = null)
    }

    fun onNameChange(name: String) {
        _addStaffUiState.value = _addStaffUiState.value.copy(nameInput = name, errorMessage = null)
    }

    fun onEmployeeIdChange(employeeId: String) {
        _addStaffUiState.value = _addStaffUiState.value.copy(employeeIdInput = employeeId, errorMessage = null)
    }

    fun addStaff() {
        val name = _addStaffUiState.value.nameInput.trim()
        val employeeId = _addStaffUiState.value.employeeIdInput.trim()

        if (name.isEmpty() || employeeId.isEmpty()) {
            _addStaffUiState.value = _addStaffUiState.value.copy(errorMessage = "Name and Employee ID cannot be empty.")
            return
        }

        _addStaffUiState.value = _addStaffUiState.value.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val existing = attendanceRepository.getStaffByEmployeeId(employeeId)
            if (existing != null) {
                _addStaffUiState.value = _addStaffUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "A staff member with Employee ID '$employeeId' already exists."
                )
            } else {
                try {
                    attendanceRepository.insertStaff(
                        Staff(
                            name = name,
                            employeeId = employeeId
                        )
                    )
                    _addStaffUiState.value = AddStaffUiState(showDialog = false)
                } catch (e: Exception) {
                    _addStaffUiState.value = _addStaffUiState.value.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to add staff member."
                    )
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.clearSession()
            onLoggedOut()
        }
    }
}
