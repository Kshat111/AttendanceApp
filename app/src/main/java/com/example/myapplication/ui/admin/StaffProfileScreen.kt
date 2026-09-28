package com.example.myapplication.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    staffId: Long,
    onNavigateBack: () -> Unit,
    viewModel: StaffProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val staff by viewModel.staff.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val editUiState by viewModel.editUiState.collectAsState()
    val deleteUiState by viewModel.deleteUiState.collectAsState()

    LaunchedEffect(staffId) {
        viewModel.loadStaff(staffId)
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

                Spacer(modifier = Modifier.height(32.dp))

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
                                "Enrolled (${currentStaff.faceEmbeddings.size} face samples)"
                            } else {
                                "Not Enrolled"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentStaff.faceEmbeddings.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Face enrolment placeholder for ${currentStaff.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(
                                if (currentStaff.faceEmbeddings.isNotEmpty()) "Re-enroll Face" else "Enroll Face"
                            )
                        }
                    }
                }
            }
        }
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
