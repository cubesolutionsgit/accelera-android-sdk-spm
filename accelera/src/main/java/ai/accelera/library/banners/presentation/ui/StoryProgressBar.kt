package ai.accelera.library.banners.presentation.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import ai.accelera.library.core.constants.AcceleraColors
import ai.accelera.library.core.constants.AcceleraDimens
import ai.accelera.library.utils.dpToPx

/**
 * Progress bar for stories.
 */
class StoryProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(AcceleraColors.PROGRESS_TRACK_ALPHA, 255, 255, 255)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var animator: ValueAnimator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = dpToPx(AcceleraDimens.PROGRESS_BAR_HEIGHT_DP)
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw background
        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        // Draw fill
        val fillWidth = width * progress
        if (fillWidth > 0) {
            canvas.drawRect(0f, 0f, fillWidth, height, fillPaint)
        }
    }

    /**
     * Resets progress to 0.
     */
    fun reset() {
        animator?.cancel()
        progress = 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

