package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AttendanceDao
import com.example.myapplication.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun getAllRecords(): Flow<List<AttendanceRecord>>
    suspend fun markAttendance(record: AttendanceRecord)
}

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao
) : AttendanceRepository {
    override fun getAllRecords(): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAllRecords()
    }

    override suspend fun markAttendance(record: AttendanceRecord) {
        attendanceDao.insertRecord(record)
    }
}
