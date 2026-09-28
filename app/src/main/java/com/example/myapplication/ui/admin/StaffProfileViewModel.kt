package com.example.myapplication.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
import com.example.myapplication.data.model.Staff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditStaffUiState(
    val showDialog: Boolean = false,
    val nameInput: String = "",
    val employeeIdInput: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false
)

data class DeleteStaffUiState(
    val showDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

class StaffProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AttendanceApplication
    private val attendanceRepository = app.container.attendanceRepository

    private val _staff = MutableStateFlow<Staff?>(null)
    val staff: StateFlow<Staff?> = _staff.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editUiState = MutableStateFlow(EditStaffUiState())
    val editUiState: StateFlow<EditStaffUiState> = _editUiState.asStateFlow()

    private val _deleteUiState = MutableStateFlow(DeleteStaffUiState())
    val deleteUiState: StateFlow<DeleteStaffUiState> = _deleteUiState.asStateFlow()

    fun loadStaff(staffId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _staff.value = attendanceRepository.getStaffById(staffId)
            _isLoading.value = false
        }
    }

    fun openEditDialog() {
        val currentStaff = _staff.value ?: return
        _editUiState.value = EditStaffUiState(
            showDialog = true,
            nameInput = currentStaff.name,
            employeeIdInput = currentStaff.employeeId
        )
    }

    fun dismissEditDialog() {
        _editUiState.value = _editUiState.value.copy(showDialog = false, errorMessage = null)
    }

    fun onNameChange(name: String) {
        _editUiState.value = _editUiState.value.copy(nameInput = name, errorMessage = null)
    }

    fun onEmployeeIdChange(employeeId: String) {
        _editUiState.value = _editUiState.value.copy(employeeIdInput = employeeId, errorMessage = null)
    }

    fun updateStaff() {
        val currentStaff = _staff.value ?: return
        val name = _editUiState.value.nameInput.trim()
        val employeeId = _editUiState.value.employeeIdInput.trim()

        if (name.isEmpty() || employeeId.isEmpty()) {
            _editUiState.value = _editUiState.value.copy(errorMessage = "Name and Employee ID cannot be empty.")
            return
        }

        _editUiState.value = _editUiState.value.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val existing = attendanceRepository.getStaffByEmployeeId(employeeId)
            if (existing != null && existing.id != currentStaff.id) {
                _editUiState.value = _editUiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "A staff member with Employee ID '$employeeId' already exists."
                )
            } else {
                try {
                    val updated = currentStaff.copy(name = name, employeeId = employeeId)
                    attendanceRepository.updateStaff(updated)
                    _staff.value = updated
                    _editUiState.value = EditStaffUiState(showDialog = false)
                } catch (e: Exception) {
                    _editUiState.value = _editUiState.value.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to update staff member."
                    )
                }
            }
        }
    }

    fun openDeleteDialog() {
        _deleteUiState.value = DeleteStaffUiState(showDialog = true)
    }

    fun dismissDeleteDialog() {
        _deleteUiState.value = _deleteUiState.value.copy(showDialog = false, errorMessage = null)
    }

    fun deleteStaff(onDeleted: () -> Unit) {
        val currentStaff = _staff.value ?: return
        _deleteUiState.value = _deleteUiState.value.copy(isDeleting = true, errorMessage = null)

        viewModelScope.launch {
            try {
                attendanceRepository.deleteStaff(currentStaff)
                _deleteUiState.value = DeleteStaffUiState(showDialog = false)
                onDeleted()
            } catch (e: Exception) {
                _deleteUiState.value = _deleteUiState.value.copy(
                    isDeleting = false,
                    errorMessage = e.message ?: "Failed to delete staff member."
                )
            }
        }
    }
}
