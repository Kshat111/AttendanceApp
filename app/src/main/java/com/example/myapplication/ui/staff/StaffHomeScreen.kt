package com.example.myapplication.ui.staff

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.myapplication.util.Constants
import com.example.myapplication.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    capturedPhotoData: Pair<String, String>? = null,
    onOpenAttendanceCamera: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: StaffHomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsState()
    val selectedStaff by viewModel.selectedStaff.collectAsState()
    val latestAttendance by viewModel.latestAttendance.collectAsState()
    val attendanceHistory by viewModel.attendanceHistory.collectAsState()
    val remainingCooldownSeconds by viewModel.remainingCooldownSeconds.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val attendanceResultState by viewModel.attendanceResultState.collectAsState()

    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var selectedAttendanceForDetail by remember { mutableStateOf<Attendance?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onOpenAttendanceCamera("attendance")
    }

    LaunchedEffect(capturedPhotoData) {
        if (capturedPhotoData != null) {
            viewModel.processAttendancePhoto(context, capturedPhotoData)
        }
    }

    val isCooldownActive = remainingCooldownSeconds > 0L
    val minutes = remainingCooldownSeconds / 60
    val seconds = remainingCooldownSeconds % 60
    val timeFormatted = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Portal") },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Staff Profile Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome, ${selectedStaff?.name ?: userName ?: "Staff"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (selectedStaff != null) {
                            Text(
                                text = "Employee ID: ${selectedStaff!!.employeeId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedStaff == null) {
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
                            text = "No staff profile found for Employee ID '$userName'",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please ask an Admin to register you as a staff member with Employee ID '$userName'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                val currentStaff = selectedStaff!!
                val isEnrolled = currentStaff.faceEmbeddings.isNotEmpty()

                // Enrollment Status Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Face Enrollment Status",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnrolled) "Enrolled (${currentStaff.faceEmbeddings.size} samples)" else "Not Enrolled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEnrolled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        if (!isEnrolled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You must be enrolled by an Admin before you can mark attendance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Live Cooldown Status Card
                if (isCooldownActive) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Attendance Recently Marked",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (latestAttendance != null) {
                                Text(
                                    text = "Last marked: ${formatTimestamp(latestAttendance!!.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please wait $timeFormatted before marking again.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verifying face, fetching location, and marking attendance...",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    // Mark Attendance Action Button
                    Button(
                        onClick = {
                            if (!LocationHelper.hasLocationPermission(context)) {
                                showLocationRationaleDialog = true
                            } else {
                                onOpenAttendanceCamera("attendance")
                            }
                        },
                        enabled = isEnrolled && !isCooldownActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isCooldownActive) "Cooldown Active ($timeFormatted)" else "Mark Attendance",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Attendance History Section for Staff
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "My Attendance History (${attendanceHistory.size})",
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
                                text = "Your attendance history will appear here once marked.",
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
                            AttendanceItemCard(
                                attendance = attendance,
                                onClick = { selectedAttendanceForDetail = attendance }
                            )
                        }
                    }
                }
            }
        }
    }

    // Location Permission Rationale Dialog
    if (showLocationRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showLocationRationaleDialog = false
                onOpenAttendanceCamera("attendance")
            },
            title = { Text("Location Access Required") },
            text = {
                Text(
                    text = "To attach GPS location coordinates to your attendance record, please grant location access. If denied, attendance can still be marked without location.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationRationaleDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationRationaleDialog = false
                        onOpenAttendanceCamera("attendance")
                    }
                ) {
                    Text("Continue Without Location")
                }
            }
        )
    }

    // Full-screen Selfie Detail View
    if (selectedAttendanceForDetail != null) {
        AttendanceDetailDialog(
            attendance = selectedAttendanceForDetail!!,
            onDismiss = { selectedAttendanceForDetail = null }
        )
    }

    // Success Screen Dialog
    if (attendanceResultState is AttendanceResultState.Success) {
        val success = attendanceResultState as AttendanceResultState.Success
        AttendanceSuccessDialog(
            success = success,
            onDone = viewModel::dismissResultState
        )
    }

    // Failure Dialog
    if (attendanceResultState is AttendanceResultState.Failure) {
        val failure = attendanceResultState as AttendanceResultState.Failure
        AttendanceFailureDialog(
            reason = failure.reason,
            onTryAgain = {
                viewModel.dismissResultState()
                onOpenAttendanceCamera("attendance")
            },
            onDismiss = viewModel::dismissResultState
        )
    }
}

@Composable
fun AttendanceItemCard(
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
fun AttendanceSuccessDialog(
    success: AttendanceResultState.Success,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDone,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Marked!",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Attendance Marked Successfully",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                AsyncImage(
                    model = success.selfiePath,
                    contentDescription = "Attendance Selfie",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = success.staffName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Employee ID: ${success.employeeId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatTimestamp(success.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // GPS Location Display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (success.latitude != null && success.longitude != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (success.latitude != null && success.longitude != null) {
                        Text(
                            text = String.format(Locale.US, "GPS: %.4f, %.4f", success.latitude, success.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Location unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Match Confidence: ${(success.matchScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun AttendanceFailureDialog(
    reason: String,
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attendance Failed")
            }
        },
        text = {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onTryAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}
