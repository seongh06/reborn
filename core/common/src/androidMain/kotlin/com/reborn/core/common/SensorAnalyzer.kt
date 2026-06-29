package com.reborn.core.common

import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class SensorAnalyzer(private val context: Context) {

    actual suspend fun analyze(): AnalysisResult = suspendCancellableCoroutine { cont ->
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
                        val bitmap = image.toBitmap()
                        image.close()
                        cameraProvider.unbindAll()

                        val lux = calculateLux(bitmap)
                        countFaces(bitmap) { count ->
                            cont.resume(AnalysisResult(count, lux))
                        }
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

    private fun countFaces(bitmap: Bitmap, onResult: (Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.1f)
            .build()
        FaceDetection.getClient(options)
            .process(image)
            .addOnSuccessListener { faces -> onResult(faces.size) }
            .addOnFailureListener { onResult(0) }
    }
}
