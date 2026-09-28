package com.example.myapplication.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromFloatArrayList(embeddings: List<FloatArray>?): String {
        if (embeddings == null) return "[]"
        val rootArray = JSONArray()
        for (floatArray in embeddings) {
            val innerArray = JSONArray()
            for (value in floatArray) {
                innerArray.put(value.toDouble())
            }
            rootArray.put(innerArray)
        }
        return rootArray.toString()
    }

    @TypeConverter
    fun toFloatArrayList(value: String?): List<FloatArray> {
        if (value.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<FloatArray>()
        try {
            val rootArray = JSONArray(value)
            for (i in 0 until rootArray.length()) {
                val innerArray = rootArray.getJSONArray(i)
                val floatArray = FloatArray(innerArray.length())
                for (j in 0 until innerArray.length()) {
                    floatArray[j] = innerArray.getDouble(j).toFloat()
                }
                result.add(floatArray)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
