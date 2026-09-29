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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Staff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onStaffClick: (Long) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val addStaffUiState by viewModel.addStaffUiState.collectAsState()

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
