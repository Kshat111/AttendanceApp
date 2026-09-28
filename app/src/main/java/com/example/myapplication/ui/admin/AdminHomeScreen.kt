package com.example.myapplication.ui.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.myapplication.data.model.Staff
import com.example.myapplication.face.FaceDetectionResult
import com.example.myapplication.face.FaceEmbeddingManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    capturedPhotoData: Pair<String, String>? = null,
    onOpenTestCamera: (String) -> Unit,
    onStaffClick: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val addStaffUiState by viewModel.addStaffUiState.collectAsState()
    val testUiState by viewModel.embeddingTestUiState.collectAsState()

    LaunchedEffect(capturedPhotoData) {
        if (capturedPhotoData != null) {
            viewModel.processCapturedPhoto(context, capturedPhotoData)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.logout(onLogout) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddStaffDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Staff"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Welcome Header Item
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome, ${userName ?: "Administrator"}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Role: Admin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Face Embedding & Cosine Similarity Test Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Face Embedding & Similarity Test",
                                style = MaterialTheme.typography.titleSmall
                            )

                            if (testUiState.resultA != null || testUiState.resultB != null) {
                                TextButton(onClick = viewModel::resetTest) {
                                    Text("Reset")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slot A Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = { onOpenTestCamera("A") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Photo A")
                                    }

                                    if (testUiState.isProcessingA) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else when (val resA = testUiState.resultA) {
                                        is FaceDetectionResult.Success -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AsyncImage(
                                                model = resA.croppedFacePath,
                                                contentDescription = "Face A",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Dim: ${resA.faceEmbedding.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        is FaceDetectionResult.Failure -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = resA.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        null -> {}
                                    }
                                }
                            }

                            // Slot B Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        onClick = { onOpenTestCamera("B") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Photo B")
                                    }

                                    if (testUiState.isProcessingB) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else when (val resB = testUiState.resultB) {
                                        is FaceDetectionResult.Success -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AsyncImage(
                                                model = resB.croppedFacePath,
                                                contentDescription = "Face B",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Dim: ${resB.faceEmbedding.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        is FaceDetectionResult.Failure -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = resB.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        null -> {}
                                    }
                                }
                            }
                        }

                        // Similarity Result Section
                        if (testUiState.resultA is FaceDetectionResult.Success && testUiState.resultB is FaceDetectionResult.Success) {
                            val embeddingA = (testUiState.resultA as FaceDetectionResult.Success).faceEmbedding
                            val embeddingB = (testUiState.resultB as FaceDetectionResult.Success).faceEmbedding
                            val similarity = FaceEmbeddingManager.cosineSimilarity(embeddingA, embeddingB)
                            val isMatch = similarity >= 0.65f

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isMatch) "MATCH (Same Person)" else "NO MATCH (Different Persons)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = String.format(Locale.US, "Cosine Similarity: %.4f", similarity),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Staff Header Item
            item {
                Text(
                    text = "Staff Members (${staffList.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Staff List / Empty State Items
            if (staffList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No staff members found",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ Add Staff' to enroll a new staff member.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(
                    items = staffList,
                    key = { it.id }
                ) { staff ->
                    StaffItemCard(
                        staff = staff,
                        onClick = { onStaffClick(staff.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (addStaffUiState.showDialog) {
        AddStaffDialog(
            uiState = addStaffUiState,
            onNameChange = viewModel::onNameChange,
            onEmployeeIdChange = viewModel::onEmployeeIdChange,
            onDismiss = viewModel::dismissAddStaffDialog,
            onConfirm = viewModel::addStaff
        )
    }
}

@Composable
fun StaffItemCard(
    staff: Staff,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = staff.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "ID: ${staff.employeeId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (staff.faceEmbeddings.isNotEmpty()) "Enrolled" else "No Face",
                style = MaterialTheme.typography.labelSmall,
                color = if (staff.faceEmbeddings.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AddStaffDialog(
    uiState: AddStaffUiState,
    onNameChange: (String) -> Unit,
    onEmployeeIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Staff Member") },
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
                    Text("Add")
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
