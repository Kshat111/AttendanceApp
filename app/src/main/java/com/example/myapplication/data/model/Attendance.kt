package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = Staff::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["staffId"])]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val selfiePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
