package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.model.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffById(id: Long): Staff?

    @Query("SELECT * FROM staff WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getStaffByEmployeeId(employeeId: String): Staff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: Staff): Long

    @Update
    suspend fun updateStaff(staff: Staff)

    @Delete
    suspend fun deleteStaff(staff: Staff)
}
