package com.multiversalcopy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureService : Service() {

    private var captureMode = CaptureMode.OFF
    private var screenCapture: ScreenCapture? = null
    private val overlayManager = OverlayManager()
    private var mediaProjection: MediaProjection? = null
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var apiUrl: String = ""

    companion object {
        const val ACTION_START = "com.multiversalcopy.ACTION_START"
        const val ACTION_CAPTURE = "com.multiversalcopy.ACTION_CAPTURE"
        const val ACTION_STOP = "com.multiversalcopy.ACTION_STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_API_URL = "api_url"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "capture_mode_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> handleStart(intent)
            ACTION_CAPTURE -> handleCapture()
            ACTION_STOP -> handleStop()
        }

        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        if (captureMode != CaptureMode.OFF) return

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        apiUrl = intent.getStringExtra(EXTRA_API_URL) ?: ""

        if (resultData == null || apiUrl.isEmpty()) {
            stopSelf()
            return
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, resultData)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = windowManager.currentWindowMetrics

        screenCapture = ScreenCapture(this, mediaProjection!!, metrics)

        captureMode = CaptureMode.ACTIVE
        Toast.makeText(this, "Capture Mode Started", Toast.LENGTH_SHORT).show()
    }

    private fun handleCapture() {
        if (captureMode != CaptureMode.ACTIVE) return
        
        captureMode = CaptureMode.CAPTURING
        
        serviceScope.launch {
            // Delay to allow the notification shade to fully retract
            kotlinx.coroutines.delay(1000)

            val imageBytes = screenCapture?.captureFrame()
            
            if (imageBytes == null) {
                captureMode = CaptureMode.ACTIVE
                Toast.makeText(this@CaptureService, "Failed to capture frame", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            captureMode = CaptureMode.UPLOADING
            Toast.makeText(this@CaptureService, "Analyzing screen...", Toast.LENGTH_SHORT).show()
            
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val params = mapOf(
                "temperature" to prefs.getFloat("temperature", 0.0f).toString(),
                "top_p" to prefs.getFloat("top_p", 1.0f).toString(),
                "repetition_penalty" to prefs.getFloat("repetition_penalty", 1.0f).toString(),
                "layout_threshold" to prefs.getFloat("layout_threshold", 0.5f).toString(),
                "max_new_tokens" to prefs.getInt("max_new_tokens", 2048).toString(),
                "layout_nms" to prefs.getBoolean("layout_nms", true).toString(),
                "use_doc_orientation_classify" to prefs.getBoolean("use_doc_orientation_classify", false).toString(),
                "use_doc_unwarping" to prefs.getBoolean("use_doc_unwarping", false).toString(),
                "use_layout_detection" to prefs.getBoolean("use_layout_detection", true).toString(),
                "use_chart_recognition" to prefs.getBoolean("use_chart_recognition", false).toString(),
                "use_seal_recognition" to prefs.getBoolean("use_seal_recognition", false).toString(),
                "use_ocr_for_image_block" to prefs.getBoolean("use_ocr_for_image_block", false).toString(),
                "format_block_content" to prefs.getBoolean("format_block_content", true).toString(),
                "merge_layout_blocks" to prefs.getBoolean("merge_layout_blocks", true).toString()
            )
            
            val response = ApiClient.uploadScreenshot(apiUrl, imageBytes, params)
            
            if (!response.success) {
                captureMode = CaptureMode.ACTIVE
                Toast.makeText(this@CaptureService, "Error: ${response.error}", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // Map coordinates and content
            val detectedItems = response.boxes.mapNotNull { box ->
                if (box.coordinate.size == 4) {
                    val rect = Rect(
                        box.coordinate[0],
                        box.coordinate[1],
                        box.coordinate[2],
                        box.coordinate[3]
                    )
                    DetectedItem(rect, box.label, box.content)
                } else null
            }
            
            if (detectedItems.isEmpty()) {
                captureMode = CaptureMode.ACTIVE
                Toast.makeText(this@CaptureService, "No text found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            captureMode = CaptureMode.SHOWING
            overlayManager.showOverlay(this@CaptureService, detectedItems) {
                overlayManager.removeOverlay()
                captureMode = CaptureMode.ACTIVE
            }
        }
    }

    private fun handleStop() {
        overlayManager.removeOverlay()
        screenCapture?.release()
        mediaProjection?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        captureMode = CaptureMode.OFF
        Toast.makeText(this, "Capture Mode Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capture Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the background screen capture service"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val captureIntent = Intent(this, CaptureService::class.java).apply { action = ACTION_CAPTURE }
        val capturePendingIntent = PendingIntent.getService(this, 0, captureIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, CaptureService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MultiversalCopy")
            .setContentText("Tap to analyze this screen")
            .setSmallIcon(R.drawable.ic_notification) 
            .setOngoing(true)
            .setContentIntent(capturePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        handleStop()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
