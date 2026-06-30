package com.reborn.core.common

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class SensorAnalyzer(private val context: Context) {

    actual suspend fun analyze(saveImage: Boolean): AnalysisResult {
        val bitmap = captureImage()
        val savedPath = withContext(Dispatchers.IO) {
            if (saveImage) saveImageToStorage(bitmap) else null
        }
        val lux = withContext(Dispatchers.Default) { calculateLux(bitmap) }
        val count = countFaces(bitmap)
        return AnalysisResult(count, lux, savedPath)
    }

    private suspend fun captureImage(): Bitmap = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )

                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.use { it.toBitmap() }
                        cameraProvider.unbindAll()
                        cont.resume(bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cameraProvider.unbindAll()
                        cont.resumeWithException(exception)
                    }
                })

                cont.invokeOnCancellation { cameraProvider.unbindAll() }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, executor)
    }

    private fun saveImageToStorage(bitmap: Bitmap): String? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "aerometer_$timestamp.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateLux(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var totalLuminance = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalLuminance += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }

        val avgBrightness = totalLuminance / pixels.size
        return (avgBrightness / 255.0 * 10000.0).toInt()
    }

    private suspend fun countFaces(bitmap: Bitmap): Int = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.1f)
            .build()
        val detector = FaceDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { faces ->
                detector.close()
                cont.resume(faces.size)
            }
            .addOnFailureListener {
                detector.close()
                cont.resume(0)
            }
    }
}
