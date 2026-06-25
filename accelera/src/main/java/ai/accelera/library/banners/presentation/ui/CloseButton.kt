package ai.accelera.library.banners.presentation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import ai.accelera.library.core.constants.AcceleraColors
import ai.accelera.library.core.constants.AcceleraDimens
import ai.accelera.library.utils.dpToPx
import ai.accelera.library.utils.dpToPxF

/**
 * Close button view for banners.
 */
class CloseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = AcceleraDimens.CLOSE_BUTTON_STROKE_WIDTH_PX
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(AcceleraColors.CLOSE_BUTTON_BG_ALPHA, 0, 0, 0)
    }

    private val cornerRadius = AcceleraDimens.CLOSE_BUTTON_CORNER_RADIUS_PX

    init {
        setOnClickListener {
            // Remove from parent
            (parent as? android.view.ViewGroup)?.removeView(this)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = dpToPx(AcceleraDimens.CLOSE_BUTTON_SIZE_DP)
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
        val halfLength = dpToPxF(AcceleraDimens.CLOSE_BUTTON_X_HALF_LENGTH_DP)
        
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

