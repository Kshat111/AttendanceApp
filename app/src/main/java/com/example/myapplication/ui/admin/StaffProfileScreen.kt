package com.example.myapplication.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.myapplication.data.model.Attendance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    staffId: Long,
    capturedPhotoData: Pair<String, String>? = null,
    onOpenCamera: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: StaffProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val staff by viewModel.staff.collectAsState()
    val attendanceHistory by viewModel.attendanceHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editUiState by viewModel.editUiState.collectAsState()
    val deleteUiState by viewModel.deleteUiState.collectAsState()
    val enrollmentUiState by viewModel.enrollmentUiState.collectAsState()

    var selectedAttendanceForDetail by remember { mutableStateOf<Attendance?>(null) }

    LaunchedEffect(staffId) {
        viewModel.loadStaff(staffId)
    }

    LaunchedEffect(capturedPhotoData) {
        if (capturedPhotoData != null) {
            viewModel.processCapturedEnrollmentPhoto(context, capturedPhotoData, onNextCameraRequest = onOpenCamera)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (staff != null) {
                        IconButton(onClick = viewModel::openEditDialog) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Staff"
                            )
                        }
                        IconButton(onClick = viewModel::openDeleteDialog) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Staff",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (staff == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Staff member not found.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val currentStaff = staff!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentStaff.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Employee ID: ${currentStaff.employeeId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Face Enrollment Card Section
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Face Enrolment",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (currentStaff.faceEmbeddings.isNotEmpty()) {
                                "Status: Enrolled (${currentStaff.faceEmbeddings.size} samples)"
                            } else {
                                "Status: Not enrolled"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentStaff.faceEmbeddings.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )

                        if (enrollmentUiState.statusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = enrollmentUiState.statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (enrollmentUiState.isActive) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Capturing Photo ${enrollmentUiState.currentStep} of 3",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (enrollmentUiState.capturedPaths.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    enrollmentUiState.capturedPaths.forEachIndexed { index, path ->
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Sample ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                }
                            }

                            if (enrollmentUiState.isProcessing) {
                                Spacer(modifier = Modifier.height(12.dp))
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }

                        if (enrollmentUiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = enrollmentUiState.errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (enrollmentUiState.errorMessage != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { viewModel.retryCurrentStep(onOpenCamera) }) {
                                    Text("Retake Photo ${enrollmentUiState.currentStep.coerceIn(1, 3)}")
                                }
                                OutlinedButton(onClick = viewModel::cancelEnrollment) {
                                    Text("Cancel")
                                }
                            }
                        } else if (enrollmentUiState.isActive) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { onOpenCamera("enroll_${enrollmentUiState.currentStep}") }) {
                                    Text("Capture Photo ${enrollmentUiState.currentStep}")
                                }
                                OutlinedButton(onClick = viewModel::cancelEnrollment) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startEnrollmentFlow(onOpenCamera) }
                            ) {
                                Text(
                                    if (currentStaff.faceEmbeddings.isNotEmpty()) "Re-enroll Face" else "Enroll Face"
                                )
                            }
                        }
                    }
                }

                // Attendance History Section
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Attendance History (${attendanceHistory.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (attendanceHistory.isEmpty()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No attendance records yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Attendance records for ${currentStaff.name} will appear here once marked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        attendanceHistory.forEach { attendance ->
                            AttendanceHistoryItemCard(
                                attendance = attendance,
                                onClick = { selectedAttendanceForDetail = attendance }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Selfie Detail View for Admin
    if (selectedAttendanceForDetail != null) {
        AttendanceDetailDialog(
            attendance = selectedAttendanceForDetail!!,
            onDismiss = { selectedAttendanceForDetail = null }
        )
    }

    if (enrollmentUiState.showReenrollConfirmDialog) {
        ReenrollConfirmDialog(
            staffName = staff?.name ?: "",
            existingCount = staff?.faceEmbeddings?.size ?: 0,
            onDismiss = viewModel::dismissReenrollConfirmDialog,
            onConfirm = { viewModel.confirmReenroll(onOpenCamera) }
        )
    }

    if (editUiState.showDialog) {
        EditStaffDialog(
            uiState = editUiState,
            onNameChange = viewModel::onNameChange,
            onEmployeeIdChange = viewModel::onEmployeeIdChange,
            onDismiss = viewModel::dismissEditDialog,
            onConfirm = viewModel::updateStaff
        )
    }

    if (deleteUiState.showDialog) {
        DeleteStaffConfirmDialog(
            staffName = staff?.name ?: "",
            employeeId = staff?.employeeId ?: "",
            uiState = deleteUiState,
            onDismiss = viewModel::dismissDeleteDialog,
            onConfirm = {
                viewModel.deleteStaff(onDeleted = onNavigateBack)
            }
        )
    }
}

@Composable
fun AttendanceHistoryItemCard(
    attendance: Attendance,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selfie Thumbnail
            if (!attendance.selfiePath.isNullOrEmpty()) {
                AsyncImage(
                    model = attendance.selfiePath,
                    contentDescription = "Attendance Selfie",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Formatted timestamp: "10:45 AM, 28 Sep 2026"
                Text(
                    text = formatTimestamp(attendance.timestamp),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (attendance.latitude != null && attendance.longitude != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (attendance.latitude != null && attendance.longitude != null) {
                        Text(
                            text = String.format(Locale.US, "GPS: %.4f, %.4f", attendance.latitude, attendance.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Location unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailDialog(
    attendance: Attendance,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Attendance Detail") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Black
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Full Selfie Image
                if (!attendance.selfiePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = attendance.selfiePath,
                        contentDescription = "Full Attendance Selfie",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timestamp below selfie
                Text(
                    text = formatTimestamp(attendance.timestamp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location below timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (attendance.latitude != null && attendance.longitude != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Gray
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (attendance.latitude != null && attendance.longitude != null) {
                        Text(
                            text = String.format(Locale.US, "GPS: %.4f, %.4f", attendance.latitude, attendance.longitude),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Location unavailable",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ReenrollConfirmDialog(
    staffName: String,
    existingCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Re-enroll Face") },
        text = {
            Text(
                text = "$staffName is already enrolled with $existingCount face sample(s). Re-enrolling will replace the existing face samples with new ones. Do you want to continue?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Re-enroll")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditStaffDialog(
    uiState: EditStaffUiState,
    onNameChange: (String) -> Unit,
    onEmployeeIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Staff Member") },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChange,
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.employeeIdInput,
                    onValueChange = onEmployeeIdChange,
                    label = { Text("Employee ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteStaffConfirmDialog(
    staffName: String,
    employeeId: String,
    uiState: DeleteStaffUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Staff Member") },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete $staffName (ID: $employeeId)?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Warning: All attendance history records associated with this staff member will also be permanently deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (uiState.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}
