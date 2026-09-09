package com.multiversalcopy

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

class OverlayManager {
    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null

    fun showOverlay(context: Context, items: List<DetectedItem>, onItemTapped: (DetectedItem?) -> Unit) {
        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        // Remove existing if any
        removeOverlay()

        overlayView = OverlayView(context).apply {
            this.items = items
            this.onItemTapped = onItemTapped
        }

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(overlayView, params)
    }

    fun removeOverlay() {
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }
        overlayView = null
    }
}
