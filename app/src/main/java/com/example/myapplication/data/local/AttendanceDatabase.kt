package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.model.AttendanceRecord

@Database(entities = [AttendanceRecord::class], version = 1, exportSchema = false)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
