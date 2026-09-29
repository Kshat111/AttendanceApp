package com.example.myapplication.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.data.preferences.UserPreferencesRepository
import com.example.myapplication.ui.admin.AdminHomeScreen
import com.example.myapplication.ui.admin.StaffProfileScreen
import com.example.myapplication.ui.camera.CameraScreen
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.staff.StaffHomeScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val userPreferencesRepository = UserPreferencesRepository(context)
    val userRoleState by userPreferencesRepository.userRole.collectAsState(initial = "LOADING")

    if (userRoleState == "LOADING") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (userRoleState) {
        "ADMIN" -> Screen.AdminHome.route
        "STAFF" -> Screen.StaffHome.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = if (role == "ADMIN") Screen.AdminHome.route else Screen.StaffHome.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminHome.route) {
            AdminHomeScreen(
                onStaffClick = { staffId ->
                    navController.navigate(Screen.StaffProfile.createRoute(staffId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.StaffProfile.route,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getLong("staffId") ?: 0L
            val capturedData = backStackEntry.savedStateHandle.remove<Pair<String, String>>("captured_photo_slot_path")
            StaffProfileScreen(
                staffId = staffId,
                capturedPhotoData = capturedData,
                onOpenCamera = { slot ->
                    navController.navigate(Screen.Camera.createRoute(slot))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("slot") { defaultValue = "A" })
        ) { backStackEntry ->
            val slot = backStackEntry.arguments?.getString("slot") ?: "A"
            CameraScreen(
                onPhotoCaptured = { photoPath ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_photo_slot_path", Pair(slot, photoPath))
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.StaffHome.route) { backStackEntry ->
            val capturedData = backStackEntry.savedStateHandle.remove<Pair<String, String>>("captured_photo_slot_path")
            StaffHomeScreen(
                capturedPhotoData = capturedData,
                onOpenAttendanceCamera = { slot ->
                    navController.navigate(Screen.Camera.createRoute(slot))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.StaffHome.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
