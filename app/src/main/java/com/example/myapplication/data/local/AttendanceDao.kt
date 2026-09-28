package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myapplication.data.model.Attendance
import com.example.myapplication.data.model.AttendanceWithStaff
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Transaction
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendanceWithStaff(): Flow<List<AttendanceWithStaff>>

    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY timestamp DESC")
    fun getAttendanceForStaff(staffId: Long): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)
}
