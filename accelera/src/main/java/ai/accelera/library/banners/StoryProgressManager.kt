package ai.accelera.library.banners

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.json.JSONObject

/**
 * Manages story progress bars and timers.
 * Handles progress animation, pause/resume, and proper cleanup.
 */
class StoryProgressManager(
    private val context: Context,
    private val progressContainer: ViewGroup,
    private val onProgressComplete: () -> Unit
) {
    private val progressBars = mutableListOf<StoryProgressBar>()
    private var progressAnimator: ValueAnimator? = null
    private var isPaused: Boolean = false
    private var pauseStartTime: Long = 0
    private var pausedDuration: Long = 0
    private var currentCardIndex: Int = -1

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && currentCardIndex >= 0 && currentCardIndex < progressBars.size) {
                updateProgress()
                handler.postDelayed(this, 16) // ~60fps
            }
        }
    }

    /**
     * Sets up progress bars for the given cards.
     */
    fun setupProgressBars(cards: List<JSONObject>, hasDuration: (JSONObject) -> Boolean) {
        stopProgress()
        progressContainer.removeAllViews()
        progressBars.clear()

        val hasAnyDuration = cards.any { hasDuration(it) }
        if (!hasAnyDuration) return

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Don't intercept touch events
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }

        cards.forEach { _ ->
            val bar = StoryProgressBar(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    (2 * context.resources.displayMetrics.density).toInt(),
                    1f
                ).apply {
                    marginEnd = (4 * context.resources.displayMetrics.density).toInt()
                }
                // Don't intercept touch events
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
            }
            container.addView(bar)
            progressBars.add(bar)
        }

        progressContainer.addView(container)
    }

    /**
     * Shows a specific card and starts its progress animation.
     */
    fun showCard(index: Int, durationMs: Long) {
        // Stop any existing progress
        stopProgress()

        if (index < 0 || index >= progressBars.size) {
            currentCardIndex = -1
            return
        }

        currentCardIndex = index

        // Reset all progress bars
        progressBars.forEachIndexed { i, bar ->
            bar.progress = if (i < index) 1f else 0f
        }

        // Reset pause state
        isPaused = false
        pausedDuration = 0
        pauseStartTime = 0

        // Start progress animation
        if (durationMs > 0) {
            startProgressAnimation(index, durationMs)
        }
    }

    /**
     * Starts progress animation for a specific card.
     */
    private fun startProgressAnimation(cardIndex: Int, durationMs: Long) {
        if (cardIndex >= progressBars.size) return

        val bar = progressBars[cardIndex]
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = durationMs
            addUpdateListener { animator ->
                if (!isPaused && currentCardIndex == cardIndex) {
                    val progress = animator.animatedValue as Float
                    bar.progress = progress
                    if (progress >= 1f) {
                        onProgressComplete()
                    }
                }
            }
            start()
        }
    }

    /**
     * Pauses progress animation.
     */
    fun pauseProgress() {
        if (isPaused) return
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        progressAnimator?.pause()
        handler.removeCallbacks(progressUpdateRunnable)
    }

    /**
     * Resumes progress animation.
     */
    fun resumeProgress() {
        if (!isPaused) return
        if (currentCardIndex < 0 || currentCardIndex >= progressBars.size) return
        isPaused = false
        pausedDuration += System.currentTimeMillis() - pauseStartTime
        progressAnimator?.resume()
    }

    /**
     * Stops progress animation and cleans up.
     */
    fun stopProgress() {
        handler.removeCallbacks(progressUpdateRunnable)
        progressAnimator?.cancel()
        progressAnimator = null
        isPaused = false
        pausedDuration = 0
        pauseStartTime = 0
        currentCardIndex = -1
    }

    /**
     * Updates progress (called by handler for smooth updates).
     */
    private fun updateProgress() {
        // The actual animation is handled by ValueAnimator
        // This method can be used for additional smooth updates if needed
    }

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        stopProgress()
        progressBars.clear()
    }
}

