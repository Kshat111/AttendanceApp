package com.example.myapplication.ui.admin

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
import com.example.myapplication.data.model.Staff
import com.example.myapplication.face.FaceDetectionResult
import com.example.myapplication.face.FaceDetectorManager
import com.example.myapplication.face.FaceEmbeddingManager
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

data class FaceEnrollmentUiState(
    val isActive: Boolean = false,
    val currentStep: Int = 0, // 1, 2, 3
    val capturedEmbeddings: List<FloatArray> = emptyList(),
    val capturedPaths: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showReenrollConfirmDialog: Boolean = false
)

class StaffProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AttendanceApplication
    private val attendanceRepository = app.container.attendanceRepository
    private val faceDetectorManager = FaceDetectorManager()

    private val _staff = MutableStateFlow<Staff?>(null)
    val staff: StateFlow<Staff?> = _staff.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editUiState = MutableStateFlow(EditStaffUiState())
    val editUiState: StateFlow<EditStaffUiState> = _editUiState.asStateFlow()

    private val _deleteUiState = MutableStateFlow(DeleteStaffUiState())
    val deleteUiState: StateFlow<DeleteStaffUiState> = _deleteUiState.asStateFlow()

    private val _enrollmentUiState = MutableStateFlow(FaceEnrollmentUiState())
    val enrollmentUiState: StateFlow<FaceEnrollmentUiState> = _enrollmentUiState.asStateFlow()

    private var lastProcessedPhotoData: Pair<String, String>? = null

    fun loadStaff(staffId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _staff.value = attendanceRepository.getStaffById(staffId)
            _isLoading.value = false
        }
    }

    fun startEnrollmentFlow(onOpenCamera: (String) -> Unit) {
        val currentStaff = _staff.value ?: return
        if (currentStaff.faceEmbeddings.isNotEmpty()) {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(showReenrollConfirmDialog = true)
        } else {
            initiateEnrollment(onOpenCamera)
        }
    }

    fun dismissReenrollConfirmDialog() {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(showReenrollConfirmDialog = false)
    }

    fun confirmReenroll(onOpenCamera: (String) -> Unit) {
        _enrollmentUiState.value = _enrollmentUiState.value.copy(showReenrollConfirmDialog = false)
        initiateEnrollment(onOpenCamera)
    }

    private fun initiateEnrollment(onOpenCamera: (String) -> Unit) {
        lastProcessedPhotoData = null
        _enrollmentUiState.value = FaceEnrollmentUiState(
            isActive = true,
            currentStep = 1,
            capturedEmbeddings = emptyList(),
            capturedPaths = emptyList(),
            statusMessage = "Photo 1 of 3: Please take selfie 1"
        )
        onOpenCamera("enroll_1")
    }

    fun cancelEnrollment() {
        lastProcessedPhotoData = null
        _enrollmentUiState.value = FaceEnrollmentUiState()
    }

    fun retryCurrentStep(onOpenCamera: (String) -> Unit) {
        val step = _enrollmentUiState.value.currentStep.coerceIn(1, 3)
        _enrollmentUiState.value = _enrollmentUiState.value.copy(errorMessage = null)
        onOpenCamera("enroll_$step")
    }

    fun processCapturedEnrollmentPhoto(
        context: Context,
        capturedData: Pair<String, String>?,
        onNextCameraRequest: (String) -> Unit
    ) {
        if (capturedData == null || capturedData == lastProcessedPhotoData) return
        lastProcessedPhotoData = capturedData

        val (slot, photoPath) = capturedData
        if (!slot.startsWith("enroll_")) return

        val step = slot.removePrefix("enroll_").toIntOrNull() ?: _enrollmentUiState.value.currentStep

        viewModelScope.launch {
            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                isProcessing = true,
                errorMessage = null,
                statusMessage = "Processing Photo $step of 3..."
            )

            when (val result = faceDetectorManager.processPhoto(context, photoPath)) {
                is FaceDetectionResult.Failure -> {
                    _enrollmentUiState.value = _enrollmentUiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Photo $step rejected: ${result.reason}"
                    )
                }
                is FaceDetectionResult.Success -> {
                    val newEmbeddings = _enrollmentUiState.value.capturedEmbeddings + listOf(result.faceEmbedding)
                    val newPaths = _enrollmentUiState.value.capturedPaths + listOf(result.croppedFacePath)

                    if (newEmbeddings.size < 3) {
                        val nextStep = newEmbeddings.size + 1
                        _enrollmentUiState.value = _enrollmentUiState.value.copy(
                            isProcessing = false,
                            currentStep = nextStep,
                            capturedEmbeddings = newEmbeddings,
                            capturedPaths = newPaths,
                            statusMessage = "Photo ${newEmbeddings.size} of 3 captured! Preparing for photo $nextStep..."
                        )
                        onNextCameraRequest("enroll_$nextStep")
                    } else {
                        // 3 Photos Captured: Perform Pairwise Similarity Consistency Check
                        val e1 = newEmbeddings[0]
                        val e2 = newEmbeddings[1]
                        val e3 = newEmbeddings[2]

                        val sim12 = FaceEmbeddingManager.cosineSimilarity(e1, e2)
                        val sim13 = FaceEmbeddingManager.cosineSimilarity(e1, e3)
                        val sim23 = FaceEmbeddingManager.cosineSimilarity(e2, e3)

                        val minThreshold = 0.60f

                        if (sim12 < minThreshold || sim13 < minThreshold || sim23 < minThreshold) {
                            _enrollmentUiState.value = _enrollmentUiState.value.copy(
                                isProcessing = false,
                                errorMessage = "The 3 captured face photos do not match each other (different person or inconsistent posture detected). Please retake enrollment.",
                                capturedEmbeddings = emptyList(),
                                capturedPaths = emptyList(),
                                currentStep = 1
                            )
                        } else {
                            // All 3 match! Save embeddings to Staff record
                            val currentStaff = _staff.value
                            if (currentStaff != null) {
                                try {
                                    val updatedStaff = currentStaff.copy(faceEmbeddings = newEmbeddings)
                                    attendanceRepository.updateStaff(updatedStaff)
                                    _staff.value = updatedStaff
                                    _enrollmentUiState.value = FaceEnrollmentUiState(
                                        isActive = false,
                                        statusMessage = "Face enrollment completed successfully with 3 matching samples!"
                                    )
                                } catch (e: Exception) {
                                    _enrollmentUiState.value = _enrollmentUiState.value.copy(
                                        isProcessing = false,
                                        errorMessage = e.message ?: "Failed to save face enrollment to database."
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
