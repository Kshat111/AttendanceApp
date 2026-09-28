package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppContainer
import com.example.myapplication.di.DefaultAppContainer

class AttendanceApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
