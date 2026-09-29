package com.example.myapplication.ui.staff

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.AttendanceApplication
import com.example.myapplication.data.model.Attendance
import com.example.myapplication.data.model.Staff
import com.example.myapplication.face.FaceDetectionResult
import com.example.myapplication.face.FaceDetectorManager
import com.example.myapplication.face.FaceEmbeddingManager
import com.example.myapplication.util.Constants
import com.example.myapplication.util.FileUtils
import com.example.myapplication.util.LocationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface AttendanceResultState {
    data class Success(
        val staffName: String,
        val employeeId: String,
        val timestamp: Long,
        val selfiePath: String,
        val matchScore: Float,
        val latitude: Double?,
        val longitude: Double?
    ) : AttendanceResultState

    data class Failure(val reason: String) : AttendanceResultState
}

@OptIn(ExperimentalCoroutinesApi::class)
class StaffHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AttendanceApplication
    private val attendanceRepository = app.container.attendanceRepository
    private val userPreferencesRepository = app.container.userPreferencesRepository
    private val faceDetectorManager = FaceDetectorManager()

    val userName: StateFlow<String?> = userPreferencesRepository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _selectedStaff = MutableStateFlow<Staff?>(null)
    val selectedStaff: StateFlow<Staff?> = _selectedStaff.asStateFlow()

    private val _latestAttendance = MutableStateFlow<Attendance?>(null)
    val latestAttendance: StateFlow<Attendance?> = _latestAttendance.asStateFlow()

    val attendanceHistory: StateFlow<List<Attendance>> = _selectedStaff.flatMapLatest { staff ->
        if (staff != null) {
            attendanceRepository.getAttendanceForStaff(staff.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _remainingCooldownSeconds = MutableStateFlow(0L)
    val remainingCooldownSeconds: StateFlow<Long> = _remainingCooldownSeconds.asStateFlow()

    private val _attendanceResultState = MutableStateFlow<AttendanceResultState?>(null)
    val attendanceResultState: StateFlow<AttendanceResultState?> = _attendanceResultState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var lastProcessedPhotoData: Pair<String, String>? = null
    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch {
            userName.collect { empId ->
                if (!empId.isNullOrEmpty()) {
                    val staff = attendanceRepository.getStaffByEmployeeId(empId)
                        ?: attendanceRepository.getAllStaff().firstOrNull()?.firstOrNull()
                    _selectedStaff.value = staff
                    if (staff != null) {
                        observeLatestAttendance(staff.id)
                    }
                }
            }
        }
    }

    private fun observeLatestAttendance(staffId: Long) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceForStaff(staffId).collect { records ->
                val latest = records.firstOrNull()
                _latestAttendance.value = latest
                startLiveCooldownTimer(latest?.timestamp ?: 0L)
            }
        }
    }

    private fun startLiveCooldownTimer(lastTimestamp: Long) {
        cooldownJob?.cancel()
        if (lastTimestamp <= 0L) {
            _remainingCooldownSeconds.value = 0L
            return
        }

        cooldownJob = viewModelScope.launch {
            while (isActive) {
                val elapsedMillis = System.currentTimeMillis() - lastTimestamp
                val remainingMillis = Constants.ATTENDANCE_COOLDOWN_MILLIS - elapsedMillis

                if (remainingMillis <= 0) {
                    _remainingCooldownSeconds.value = 0L
                    break
                } else {
                    _remainingCooldownSeconds.value = (remainingMillis + 999) / 1000L
                    delay(1000L)
                }
            }
        }
    }

    fun processAttendancePhoto(context: Context, capturedData: Pair<String, String>?) {
        if (capturedData == null || capturedData == lastProcessedPhotoData) return
        lastProcessedPhotoData = capturedData

        val (slot, photoPath) = capturedData
        if (slot != "attendance") return

        val staffMember = _selectedStaff.value
        if (staffMember == null) {
            _attendanceResultState.value = AttendanceResultState.Failure("No staff profile found for logged-in user.")
            return
        }

        if (staffMember.faceEmbeddings.isEmpty()) {
            _attendanceResultState.value = AttendanceResultState.Failure("Your face is not enrolled yet. Please ask an Admin to enroll your face first.")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _attendanceResultState.value = null

            when (val result = faceDetectorManager.processPhoto(context, photoPath)) {
                is FaceDetectionResult.Failure -> {
                    _isProcessing.value = false
                    _attendanceResultState.value = AttendanceResultState.Failure(result.reason)
                }
                is FaceDetectionResult.Success -> {
                    val queryEmbedding = result.faceEmbedding
                    val scores = staffMember.faceEmbeddings.map { enrolled ->
                        FaceEmbeddingManager.cosineSimilarity(queryEmbedding, enrolled)
                    }
                    val maxScore = scores.maxOrNull() ?: 0.0f

                    if (maxScore < Constants.FACE_MATCH_THRESHOLD) {
                        _isProcessing.value = false
                        val scorePercent = (maxScore * 100).toInt()
                        val reqPercent = (Constants.FACE_MATCH_THRESHOLD * 100).toInt()
                        _attendanceResultState.value = AttendanceResultState.Failure(
                            "Face does not match enrolled profile ($scorePercent% similarity, required $reqPercent%). Please try again."
                        )
                    } else {
                        // Match Success!
                        // 1. Copy selfie from cache to permanent filesDir
                        val permanentSelfiePath = FileUtils.copyToPermanentStorage(context, result.croppedFacePath)

                        // 2. Fetch real GPS Location (if permitted & available)
                        val locResult = LocationHelper.getCurrentLocation(context)

                        try {
                            val attendance = Attendance(
                                staffId = staffMember.id,
                                timestamp = System.currentTimeMillis(),
                                selfiePath = permanentSelfiePath,
                                latitude = locResult.latitude,
                                longitude = locResult.longitude
                            )
                            attendanceRepository.markAttendance(attendance)
                            _latestAttendance.value = attendance
                            startLiveCooldownTimer(attendance.timestamp)
                            _isProcessing.value = false
                            _attendanceResultState.value = AttendanceResultState.Success(
                                staffName = staffMember.name,
                                employeeId = staffMember.employeeId,
                                timestamp = attendance.timestamp,
                                selfiePath = permanentSelfiePath,
                                matchScore = maxScore,
                                latitude = locResult.latitude,
                                longitude = locResult.longitude
                            )
                        } catch (e: Exception) {
                            _isProcessing.value = false
                            _attendanceResultState.value = AttendanceResultState.Failure(
                                e.message ?: "Failed to save attendance record."
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissResultState() {
        lastProcessedPhotoData = null
        _attendanceResultState.value = null
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.clearSession()
            onLoggedOut()
        }
    }
}
