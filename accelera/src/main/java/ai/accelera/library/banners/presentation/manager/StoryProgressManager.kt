package ai.accelera.library.banners.presentation.manager

import android.animation.ValueAnimator
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import ai.accelera.library.banners.presentation.ui.StoryProgressBar
import ai.accelera.library.core.constants.AcceleraDimens
import ai.accelera.library.utils.dpToPx
import org.json.JSONObject

/**
 * Manages story progress bars and timers.
 *
 * One [StoryProgressBar] is created per card; the current card's bar is driven by a
 * [ValueAnimator] whose completion triggers [onProgressComplete] (auto-advance).
 * Pause/resume rely on [ValueAnimator.pause]/[ValueAnimator.resume], which keep the
 * elapsed fraction internally.
 */
class StoryProgressManager(
    private val context: Context,
    private val progressContainer: ViewGroup,
    private val onProgressComplete: () -> Unit
) {
    private val progressBars = mutableListOf<StoryProgressBar>()
    private var progressAnimator: ValueAnimator? = null
    private var isPaused: Boolean = false
    private var currentCardIndex: Int = -1

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
                    context.dpToPx(AcceleraDimens.PROGRESS_BAR_HEIGHT_DP),
                    1f
                ).apply {
                    marginEnd = context.dpToPx(AcceleraDimens.PROGRESS_BAR_SPACING_DP)
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
     *
     * @param startFraction Fraction of the timer already elapsed (0..1), used when
     * restoring playback after the activity was recreated.
     */
    fun showCard(index: Int, durationMs: Long, startFraction: Float = 0f) {
        // Stop any existing progress
        stopProgress()

        if (index < 0 || index >= progressBars.size) {
            currentCardIndex = -1
            return
        }

        currentCardIndex = index

        val initialFraction = startFraction.coerceIn(0f, 1f)

        // Bars before the current card are full, after it — empty.
        progressBars.forEachIndexed { i, bar ->
            bar.progress = when {
                i < index -> 1f
                i == index -> initialFraction
                else -> 0f
            }
        }

        isPaused = false

        if (durationMs > 0) {
            startProgressAnimation(index, durationMs, initialFraction)
        }
    }

    /**
     * Returns the elapsed fraction (0..1) of the current card's timer, or 0 when idle.
     */
    fun currentProgressFraction(): Float {
        return (progressAnimator?.animatedValue as? Float) ?: 0f
    }

    /**
     * Starts progress animation for a specific card from the given elapsed fraction.
     */
    private fun startProgressAnimation(cardIndex: Int, durationMs: Long, startFraction: Float = 0f) {
        if (cardIndex >= progressBars.size) return

        val bar = progressBars[cardIndex]
        progressAnimator = ValueAnimator.ofFloat(startFraction, 1f).apply {
            this.duration = (durationMs * (1f - startFraction)).toLong().coerceAtLeast(1L)
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
     * Pauses progress animation; the elapsed fraction is preserved by the animator.
     */
    fun pauseProgress() {
        if (isPaused) return
        isPaused = true
        progressAnimator?.pause()
    }

    /**
     * Resumes progress animation from where it was paused.
     */
    fun resumeProgress() {
        if (!isPaused) return
        if (currentCardIndex < 0 || currentCardIndex >= progressBars.size) return
        isPaused = false
        progressAnimator?.resume()
    }

    /**
     * Stops progress animation and resets timer state (bars stay attached).
     */
    fun stopProgress() {
        progressAnimator?.cancel()
        progressAnimator?.removeAllUpdateListeners()
        progressAnimator?.removeAllListeners()
        progressAnimator = null

        isPaused = false
        currentCardIndex = -1
    }

    /**
     * Releases everything; the manager must not be used afterwards.
     */
    fun cleanup() {
        stopProgress()
        progressBars.clear()
    }
}

