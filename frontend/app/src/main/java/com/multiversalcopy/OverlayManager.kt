package com.multiversalcopy

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button

class OverlayManager {
    private var windowManager: WindowManager? = null
    private var overlayRoot: View? = null

    fun showOverlay(context: Context, items: List<DetectedItem>, onDismiss: () -> Unit) {
        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        removeOverlay()

        val inflater = LayoutInflater.from(context)
        overlayRoot = inflater.inflate(R.layout.overlay_layout, null)

        val overlayView = overlayRoot!!.findViewById<OverlayView>(R.id.overlayView)
        overlayView.items = items

        val btnCancel = overlayRoot!!.findViewById<Button>(R.id.btnCancel)
        val btnCopy = overlayRoot!!.findViewById<Button>(R.id.btnCopy)

        btnCancel.setOnClickListener {
            onDismiss()
        }

        btnCopy.setOnClickListener {
            val selectedText = overlayView.selectedIndices
                .sorted()
                .map { overlayView.items[it].content }
                .joinToString("\n\n")

            if (selectedText.isNotEmpty()) {
                val intent = Intent(context, CopyActivity::class.java).apply {
                    putExtra(CopyActivity.EXTRA_TEXT, selectedText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                }
                context.startActivity(intent)
            }
            onDismiss()
        }

        // Apply UI flags so that (0,0) matches the raw physical screen pixels,
        // avoiding shifts from the status bar or navigation bar.
        overlayRoot!!.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager?.addView(overlayRoot, params)
    }

    fun removeOverlay() {
        overlayRoot?.let {
            if (it.isAttachedToWindow) {
                windowManager?.removeView(it)
            }
        }
        overlayRoot = null
    }
}
