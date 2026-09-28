package com.example.myapplication.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AttendanceWithStaff(
    @Embedded val attendance: Attendance,
    @Relation(
        parentColumn = "staffId",
        entityColumn = "id"
    )
    val staff: Staff
)
