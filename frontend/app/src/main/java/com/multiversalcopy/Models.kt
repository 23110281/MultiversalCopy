package com.multiversalcopy

import android.graphics.Rect
import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val label: String,
    val coordinate: List<Int>, // [x1, y1, x2, y2]
    val content: String = ""
)

@Serializable
data class OcrResponse(
    val success: Boolean,
    val markdown: String = "",
    val boxes: List<BoundingBox> = emptyList(),
    val error: String? = null
)

data class DetectedItem(
    val rect: Rect,
    val label: String,
    val content: String
)

enum class CaptureMode {
    OFF,        // Service not running
    ACTIVE,     // FGS running, notification visible, waiting for user tap
    CAPTURING,  // MediaProjection is grabbing a frame
    UPLOADING,  // Screenshot sent to backend, waiting for response
    SHOWING     // Overlay is visible with bounding boxes
}
