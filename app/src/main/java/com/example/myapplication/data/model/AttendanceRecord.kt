package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String
)
