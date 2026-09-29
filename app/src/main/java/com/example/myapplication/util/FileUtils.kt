package com.example.myapplication.util

import android.content.Context
import android.util.Log
import java.io.File

object FileUtils {

    private const val TAG = "FileUtils"

    fun copyToPermanentStorage(context: Context, cacheFilePath: String): String {
        val srcFile = File(cacheFilePath)
        if (!srcFile.exists()) return cacheFilePath

        val destDir = File(context.filesDir, "attendance_selfies")
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val destFile = File(destDir, "selfie_${System.currentTimeMillis()}.jpg")
        return try {
            srcFile.copyTo(destFile, overwrite = true)
            Log.d(TAG, "Successfully copied selfie from cache to permanent filesDir: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy selfie to permanent storage", e)
            cacheFilePath
        }
    }
}
