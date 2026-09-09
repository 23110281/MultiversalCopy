package com.multiversalcopy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowMetrics
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

class ScreenCapture(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val metrics: WindowMetrics
) {
    private val width = metrics.bounds.width()
    private val height = metrics.bounds.height()
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("WrongConstant")
    suspend fun captureFrame(): ByteArray? = suspendCancellableCoroutine { continuation ->
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var isCompleted = false

        fun cleanup() {
            try {
                virtualDisplay?.release()
                imageReader?.close()
            } catch (e: Exception) {
                Log.e("ScreenCapture", "Cleanup error", e)
            }
        }

        try {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)

            imageReader.setOnImageAvailableListener({ reader ->
                if (isCompleted) return@setOnImageAvailableListener

                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        val croppedBitmap = if (rowPadding == 0) {
                            bitmap
                        } else {
                            val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                            bitmap.recycle()
                            cropped
                        }

                        image.close()

                        val outputStream = ByteArrayOutputStream()
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        croppedBitmap.recycle()
                        
                        isCompleted = true
                        cleanup()
                        continuation.resume(outputStream.toByteArray())
                    }
                } catch (e: Exception) {
                    Log.e("ScreenCapture", "Error acquiring image", e)
                    if (!isCompleted) {
                        isCompleted = true
                        cleanup()
                        continuation.resume(null)
                    }
                }
            }, handler)

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "MultiversalCopy",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

        } catch (e: Exception) {
            Log.e("ScreenCapture", "Error setting up capture", e)
            if (!isCompleted) {
                isCompleted = true
                cleanup()
                continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            if (!isCompleted) {
                isCompleted = true
                cleanup()
            }
        }
    }

    fun release() {
        // mediaProjection.stop() is handled by the CaptureService 
        // to keep it active across multiple captures.
    }
}
