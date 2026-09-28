package com.example.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

private const val TAG = "FaceEmbeddingManager"
private const val MODEL_FILE_NAME = "mobilefacenet.tflite"

class FaceEmbeddingManager(context: Context) {

    private val interpreter: Interpreter
    val inputHeight: Int
    val inputWidth: Int
    val numChannels: Int
    val inputDataType: DataType
    val embeddingDim: Int

    init {
        val modelBuffer = loadModelFile(context, MODEL_FILE_NAME)
        val options = Interpreter.Options()
        interpreter = Interpreter(modelBuffer, options)

        // Dynamically inspect input tensor shape and data type
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape() // e.g. [1, 112, 112, 3] or [1, 192, 192, 3]
        inputDataType = inputTensor.dataType()

        if (inputShape.size == 4) {
            // [batch, height, width, channels]
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]
            numChannels = inputShape[3]
        } else {
            inputHeight = 112
            inputWidth = 112
            numChannels = 3
        }

        // Dynamically inspect output tensor shape
        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape() // e.g. [1, 128] or [1, 512]
        embeddingDim = outputShape.last()

        Log.d(
            TAG,
            "Loaded model '$MODEL_FILE_NAME': inputShape=${inputShape.contentToString()}, " +
                    "inputDataType=$inputDataType, outputShape=${outputShape.contentToString()}, " +
                    "embeddingDim=$embeddingDim"
        )
    }

    fun extractEmbedding(croppedFaceBitmap: Bitmap): FloatArray {
        // Resize bitmap to model's input size
        val resizedBitmap = Bitmap.createScaledBitmap(croppedFaceBitmap, inputWidth, inputHeight, true)

        // Prepare ByteBuffer
        val bytesPerChannel = if (inputDataType == DataType.FLOAT32) 4 else 1
        val byteBuffer = ByteBuffer.allocateDirect(bytesPerChannel * 1 * inputHeight * inputWidth * numChannels)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputWidth * inputHeight)
        resizedBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            if (inputDataType == DataType.FLOAT32) {
                // Normalize to [-1.0, 1.0] as expected by MobileFaceNet
                byteBuffer.putFloat((r - 127.5f) / 128.0f)
                byteBuffer.putFloat((g - 127.5f) / 128.0f)
                byteBuffer.putFloat((b - 127.5f) / 128.0f)
            } else {
                byteBuffer.put(r.toByte())
                byteBuffer.put(g.toByte())
                byteBuffer.put(b.toByte())
            }
        }

        // Run inference
        val outputBuffer = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(byteBuffer, outputBuffer)

        val rawEmbedding = outputBuffer[0]
        val normalizedEmbedding = l2Normalize(rawEmbedding)

        Log.d(TAG, "Extraction complete: rawFirst3=[${rawEmbedding.take(3).joinToString()}], normFirst3=[${normalizedEmbedding.take(3).joinToString()}]")

        return normalizedEmbedding
    }

    companion object {
        fun l2Normalize(embedding: FloatArray): FloatArray {
            var sum = 0.0f
            for (v in embedding) {
                sum += v * v
            }
            val norm = sqrt(sum)
            if (norm == 0.0f) return embedding

            val normalized = FloatArray(embedding.size)
            for (i in embedding.indices) {
                normalized[i] = embedding[i] / norm
            }
            return normalized
        }

        fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
            if (embedding1.size != embedding2.size || embedding1.isEmpty()) return 0.0f

            var dotProduct = 0.0f
            var normA = 0.0f
            var normB = 0.0f

            for (i in embedding1.indices) {
                val a = embedding1[i]
                val b = embedding2[i]
                dotProduct += a * b
                normA += a * a
                normB += b * b
            }

            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0.0f) 0.0f else (dotProduct / denominator)
        }
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
