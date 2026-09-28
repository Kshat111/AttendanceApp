package com.example.myapplication.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object AdminHome : Screen("admin_home")
    data object StaffHome : Screen("staff_home")
    data object StaffProfile : Screen("staff_profile/{staffId}") {
        fun createRoute(staffId: Long) = "staff_profile/$staffId"
    }
    data object Camera : Screen("camera?slot={slot}") {
        fun createRoute(slot: String = "A") = "camera?slot=$slot"
    }
}
