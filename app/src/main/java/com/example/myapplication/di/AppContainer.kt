package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.local.AttendanceDatabase
import com.example.myapplication.data.preferences.UserPreferencesRepository
import com.example.myapplication.data.repository.AttendanceRepository
import com.example.myapplication.data.repository.AttendanceRepositoryImpl

interface AppContainer {
    val attendanceRepository: AttendanceRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AttendanceDatabase by lazy {
        AttendanceDatabase.getInstance(context)
    }

    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(
            staffDao = database.staffDao(),
            attendanceDao = database.attendanceDao()
        )
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }
}
