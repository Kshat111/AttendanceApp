package com.example.myapplication.ui.admin

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
import com.example.myapplication.data.model.Staff
import com.example.myapplication.face.FaceDetectionResult
import com.example.myapplication.face.FaceDetectorManager
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

data class EmbeddingTestUiState(
    val isProcessingA: Boolean = false,
    val resultA: FaceDetectionResult? = null,
    val isProcessingB: Boolean = false,
    val resultB: FaceDetectionResult? = null
)

class AdminHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AttendanceApplication
    private val attendanceRepository = app.container.attendanceRepository
    private val userPreferencesRepository = app.container.userPreferencesRepository
    private val faceDetectorManager = FaceDetectorManager()

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

    private val _embeddingTestUiState = MutableStateFlow(EmbeddingTestUiState())
    val embeddingTestUiState: StateFlow<EmbeddingTestUiState> = _embeddingTestUiState.asStateFlow()

    private var lastProcessedPhotoData: Pair<String, String>? = null

    fun processCapturedPhoto(context: Context, capturedData: Pair<String, String>?) {
        if (capturedData == null || capturedData == lastProcessedPhotoData) return
        lastProcessedPhotoData = capturedData

        val (slot, photoPath) = capturedData
        viewModelScope.launch {
            if (slot == "A") {
                _embeddingTestUiState.value = _embeddingTestUiState.value.copy(isProcessingA = true)
                val res = faceDetectorManager.processPhoto(context, photoPath)
                _embeddingTestUiState.value = _embeddingTestUiState.value.copy(isProcessingA = false, resultA = res)
            } else if (slot == "B") {
                _embeddingTestUiState.value = _embeddingTestUiState.value.copy(isProcessingB = true)
                val res = faceDetectorManager.processPhoto(context, photoPath)
                _embeddingTestUiState.value = _embeddingTestUiState.value.copy(isProcessingB = false, resultB = res)
            }
        }
    }

    fun resetTest() {
        lastProcessedPhotoData = null
        _embeddingTestUiState.value = EmbeddingTestUiState()
    }

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
