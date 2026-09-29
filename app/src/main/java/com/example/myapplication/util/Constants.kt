package com.example.myapplication.util

object Constants {
    const val DATABASE_NAME = "attendance_db"
    const val DATASTORE_NAME = "user_preferences"
    const val FACE_MATCH_THRESHOLD = 0.65f
    const val ATTENDANCE_COOLDOWN_MINUTES = 1L
    const val ATTENDANCE_COOLDOWN_MILLIS = ATTENDANCE_COOLDOWN_MINUTES * 60 * 1000L
}
