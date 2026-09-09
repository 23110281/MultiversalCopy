package com.multiversalcopy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.graphics.RectF

class OverlayView(context: Context) : View(context) {

    var items: List<DetectedItem> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onItemTapped: ((DetectedItem?) -> Unit)? = null

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

    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }

    private val labelBgPaint = Paint().apply {
        color = Color.parseColor("#CC000000") // Dark semi-transparent
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (item in items) {
            val rect = item.rect
            // Draw fill
            canvas.drawRect(rect, fillPaint)
            // Draw stroke
            canvas.drawRect(rect, boxPaint)

            // Draw label
            val text = item.label
            val textWidth = labelPaint.measureText(text)
            val fontMetrics = labelPaint.fontMetrics
            val textHeight = fontMetrics.bottom - fontMetrics.top

            val padding = 8f
            val bgRect = RectF(
                rect.left.toFloat(),
                rect.top.toFloat() - textHeight - padding * 2,
                rect.left.toFloat() + textWidth + padding * 2,
                rect.top.toFloat()
            )
            
            // Adjust if drawing outside screen at the top
            if (bgRect.top < 0) {
                bgRect.offset(0f, rect.height().toFloat() + textHeight + padding * 2)
            }

            canvas.drawRect(bgRect, labelBgPaint)
            canvas.drawText(
                text,
                bgRect.left + padding,
                bgRect.bottom - padding - fontMetrics.bottom,
                labelPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            // Check items in reverse order (top-most first if they overlap)
            for (i in items.indices.reversed()) {
                val item = items[i]
                if (item.rect.contains(x, y)) {
                    onItemTapped?.invoke(item)
                    return true
                }
            }
            
            // No item tapped, dismiss
            onItemTapped?.invoke(null)
            return true
        }
        return true // Consume all touches while overlay is visible
    }
}
