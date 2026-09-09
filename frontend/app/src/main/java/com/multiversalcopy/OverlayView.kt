package com.multiversalcopy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var items: List<DetectedItem> = emptyList()
        set(value) {
            field = value
            selectedIndices.clear()
            invalidate()
        }

    val selectedIndices = mutableSetOf<Int>()

    // Unselected box style
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#80FF0000") // Semi-transparent red
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = Color.parseColor("#33FF0000") // Very transparent red
        style = Paint.Style.FILL
    }

    // Selected box style
    private val selectedBoxPaint = Paint().apply {
        color = Color.parseColor("#FF4CAF50") // Solid green
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val selectedFillPaint = Paint().apply {
        color = Color.parseColor("#664CAF50") // Semi-transparent green
        style = Paint.Style.FILL
    }

    // Labels removed for release build

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for ((index, item) in items.withIndex()) {
            val rect = item.rect
            val isSelected = selectedIndices.contains(index)
            
            // Draw fill & stroke
            canvas.drawRect(rect, if (isSelected) selectedFillPaint else fillPaint)
            canvas.drawRect(rect, if (isSelected) selectedBoxPaint else boxPaint)
            // Label drawing removed for release build
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            // Check items in reverse order (top-most first)
            for (i in items.indices.reversed()) {
                val item = items[i]
                if (item.rect.contains(x, y)) {
                    if (selectedIndices.contains(i)) {
                        selectedIndices.remove(i)
                    } else {
                        selectedIndices.add(i)
                    }
                    invalidate()
                    return true
                }
            }
        }
        return true // Consume touches so they don't fall through
    }
}
