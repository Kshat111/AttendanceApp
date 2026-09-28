package com.example.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

private const val TAG = "FaceDetectorManager"

sealed interface FaceDetectionResult {
    data class Success(
        val croppedFaceBitmap: Bitmap,
        val croppedFacePath: String,
        val faceEmbedding: FloatArray
    ) : FaceDetectionResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (croppedFacePath != other.croppedFacePath) return false
            if (!faceEmbedding.contentEquals(other.faceEmbedding)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = croppedFacePath.hashCode()
            result = 31 * result + faceEmbedding.contentHashCode()
            return result
        }
    }

    data class Failure(val reason: String) : FaceDetectionResult
}

class FaceDetectorManager {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(detectorOptions)
    }

    private var embeddingManager: FaceEmbeddingManager? = null

    suspend fun processPhoto(context: Context, imagePath: String): FaceDetectionResult = withContext(Dispatchers.IO) {
        val bitmap = loadAndRotateImage(imagePath)
            ?: return@withContext FaceDetectionResult.Failure("Failed to load captured image file.")

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val faces = try {
            detectFaces(inputImage)
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit face detection failed", e)
            return@withContext FaceDetectionResult.Failure("Face detection failed: ${e.localizedMessage}")
        }

        if (faces.isEmpty()) {
            return@withContext FaceDetectionResult.Failure("No face detected. Please ensure your face is clearly visible in the frame.")
        }

        if (faces.size > 1) {
            return@withContext FaceDetectionResult.Failure("Multiple faces detected (${faces.size}). Only one person should be in the frame.")
        }

        val face = faces[0]

        // Check head orientation / pose angles
        val yaw = face.headEulerAngleY   // Head turned left / right
        val pitch = face.headEulerAngleX // Head tilted up / down
        val roll = face.headEulerAngleZ  // Head tilted sideways
        val maxAllowedAngle = 20.0f

        if (abs(yaw) > maxAllowedAngle || abs(pitch) > maxAllowedAngle || abs(roll) > maxAllowedAngle) {
            return@withContext FaceDetectionResult.Failure("Please look straight at the camera without turning or tilting your head.")
        }

        // Check face size relative to the frame
        val boundingBox = face.boundingBox
        val faceWidth = boundingBox.width()
        val faceHeight = boundingBox.height()

        val minRatio = 0.20f // Face must be at least 20% of image dimensions
        if (faceWidth < bitmap.width * minRatio || faceHeight < bitmap.height * minRatio) {
            return@withContext FaceDetectionResult.Failure("Face is too far away or too small. Please move closer to the camera.")
        }

        // Apply 15% padding around the face bounding box before cropping
        val paddingX = (faceWidth * 0.15f).toInt()
        val paddingY = (faceHeight * 0.15f).toInt()

        val left = (boundingBox.left - paddingX).coerceIn(0, bitmap.width - 1)
        val top = (boundingBox.top - paddingY).coerceIn(0, bitmap.height - 1)
        val right = (boundingBox.right + paddingX).coerceIn(left + 1, bitmap.width)
        val bottom = (boundingBox.bottom + paddingY).coerceIn(top + 1, bitmap.height)

        val cropWidth = right - left
        val cropHeight = bottom - top

        val croppedBitmap = try {
            Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap cropping failed", e)
            return@withContext FaceDetectionResult.Failure("Failed to crop face region: ${e.localizedMessage}")
        }

        val croppedFile = File(context.cacheDir, "cropped_face_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Saving cropped face failed", e)
            return@withContext FaceDetectionResult.Failure("Failed to save cropped face image.")
        }

        // Initialize TFLite FaceEmbeddingManager if needed
        val manager = embeddingManager ?: synchronized(this) {
            embeddingManager ?: FaceEmbeddingManager(context).also { embeddingManager = it }
        }

        val faceEmbedding = try {
            manager.extractEmbedding(croppedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Embedding extraction failed", e)
            return@withContext FaceDetectionResult.Failure("Failed to extract face embedding: ${e.localizedMessage}")
        }

        Log.d(TAG, "Face successfully cropped (with 15% padding) and embedded (dim=${faceEmbedding.size}) at: ${croppedFile.absolutePath}")

        FaceDetectionResult.Success(
            croppedFaceBitmap = croppedBitmap,
            croppedFacePath = croppedFile.absolutePath,
            faceEmbedding = faceEmbedding
        )
    }

    private suspend fun detectFaces(inputImage: InputImage): List<Face> =
        suspendCancellableCoroutine { continuation ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (continuation.isActive) continuation.resume(faces)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        }

    private fun loadAndRotateImage(imagePath: String): Bitmap? {
        val cleanPath = when {
            imagePath.startsWith("file://") -> Uri.parse(imagePath).path ?: imagePath.removePrefix("file://")
            else -> imagePath
        }

        val file = File(cleanPath)
        Log.d(TAG, "Loading image: rawPath='$imagePath', cleanPath='$cleanPath', exists=${file.exists()}, length=${if (file.exists()) file.length() else 0} bytes")

        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $cleanPath")
            return null
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            Log.e(TAG, "BitmapFactory failed to decode file at: ${file.absolutePath}")
            return null
        }

        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            Log.d(TAG, "EXIF orientation=$orientation, rotationDegrees=$rotationDegrees, bitmapSize=${bitmap.width}x${bitmap.height}")

            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF read error or rotation skip: ${e.message}")
            bitmap
        }
    }
}
