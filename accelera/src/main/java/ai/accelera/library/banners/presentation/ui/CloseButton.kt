package ai.accelera.library.banners.presentation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Close button view for banners.
 */
class CloseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(51, 0, 0, 0) // 20% black
    }

    private val cornerRadius = 12f
    private val lineLength = 8f

    init {
        setOnClickListener {
            // Remove from parent
            (parent as? android.view.ViewGroup)?.removeView(this)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (24 * density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        
        // Draw background
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw X
        val centerX = width / 2f
        val centerY = height / 2f
        val halfLength = lineLength * density
        
        // Draw two lines forming X
        canvas.drawLine(
            centerX - halfLength,
            centerY - halfLength,
            centerX + halfLength,
            centerY + halfLength,
            paint
        )
        canvas.drawLine(
            centerX + halfLength,
            centerY - halfLength,
            centerX - halfLength,
            centerY + halfLength,
            paint
        )
    }
}

