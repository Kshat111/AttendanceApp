package com.example.myapplication.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object AdminHome : Screen("admin_home")
    data object StaffHome : Screen("staff_home")
}
