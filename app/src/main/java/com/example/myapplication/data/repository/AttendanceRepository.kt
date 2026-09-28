package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AttendanceDao
import com.example.myapplication.data.local.StaffDao
import com.example.myapplication.data.model.Attendance
import com.example.myapplication.data.model.AttendanceWithStaff
import com.example.myapplication.data.model.Staff
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    // Staff operations
    fun getAllStaff(): Flow<List<Staff>>
    suspend fun getStaffById(id: Long): Staff?
    suspend fun getStaffByEmployeeId(employeeId: String): Staff?
    suspend fun insertStaff(staff: Staff): Long
    suspend fun updateStaff(staff: Staff)
    suspend fun deleteStaff(staff: Staff)

    // Attendance operations
    fun getAllAttendance(): Flow<List<Attendance>>
    fun getAllAttendanceWithStaff(): Flow<List<AttendanceWithStaff>>
    fun getAttendanceForStaff(staffId: Long): Flow<List<Attendance>>
    suspend fun markAttendance(attendance: Attendance): Long
    suspend fun deleteAttendance(attendance: Attendance)
}

class AttendanceRepositoryImpl(
    private val staffDao: StaffDao,
    private val attendanceDao: AttendanceDao
) : AttendanceRepository {

    override fun getAllStaff(): Flow<List<Staff>> = staffDao.getAllStaff()

    override suspend fun getStaffById(id: Long): Staff? = staffDao.getStaffById(id)

    override suspend fun getStaffByEmployeeId(employeeId: String): Staff? = staffDao.getStaffByEmployeeId(employeeId)

    override suspend fun insertStaff(staff: Staff): Long = staffDao.insertStaff(staff)

    override suspend fun updateStaff(staff: Staff) = staffDao.updateStaff(staff)

    override suspend fun deleteStaff(staff: Staff) = staffDao.deleteStaff(staff)

    override fun getAllAttendance(): Flow<List<Attendance>> = attendanceDao.getAllAttendance()

    override fun getAllAttendanceWithStaff(): Flow<List<AttendanceWithStaff>> = attendanceDao.getAllAttendanceWithStaff()

    override fun getAttendanceForStaff(staffId: Long): Flow<List<Attendance>> = attendanceDao.getAttendanceForStaff(staffId)

    override suspend fun markAttendance(attendance: Attendance): Long = attendanceDao.insertAttendance(attendance)

    override suspend fun deleteAttendance(attendance: Attendance) = attendanceDao.deleteAttendance(attendance)
}
