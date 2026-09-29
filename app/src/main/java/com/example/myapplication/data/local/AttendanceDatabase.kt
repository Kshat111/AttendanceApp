package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.data.model.Attendance
import com.example.myapplication.data.model.Staff
import com.example.myapplication.util.Constants

@Database(
    entities = [Staff::class, Attendance::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun staffDao(): StaffDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getInstance(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
