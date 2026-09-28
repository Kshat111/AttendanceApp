package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff",
    indices = [Index(value = ["employeeId"], unique = true)]
)
data class Staff(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val employeeId: String,
    val faceEmbeddings: List<FloatArray> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Staff

        if (id != other.id) return false
        if (name != other.name) return false
        if (employeeId != other.employeeId) return false
        if (faceEmbeddings.size != other.faceEmbeddings.size) return false
        for (i in faceEmbeddings.indices) {
            if (!faceEmbeddings[i].contentEquals(other.faceEmbeddings[i])) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + employeeId.hashCode()
        result = 31 * result + faceEmbeddings.hashCode()
        return result
    }
}
