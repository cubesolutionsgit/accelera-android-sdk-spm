package ai.accelera.library.banners

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup

/**
 * Handles animations for story entry transitions.
 */
class StoryEntryAnimator(
    private val rootView: ViewGroup,
    private val animationDuration: Long = 300L
) {
    private var isAnimating = false

    /**
     * Animates transition to next entry (swipe left).
     * Current view goes left, new view comes from right.
     */
    fun animateToNextEntry(
        currentView: View,
        nextView: View,
        onComplete: () -> Unit
    ) {
        if (isAnimating) return
        isAnimating = true

        val screenWidth = rootView.width.toFloat()

        // Position next view to the right
        nextView.translationX = screenWidth
        nextView.visibility = View.VISIBLE

        // Animate current view out to the left
        currentView.animate()
            .translationX(-screenWidth)
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(null)

        // Animate next view in from the right
        nextView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentView.visibility = View.GONE
                    currentView.translationX = 0f
                    currentView.alpha = 1f
                    nextView.translationX = 0f
                    isAnimating = false
                    onComplete()
                }
            })
    }

    /**
     * Animates transition to previous entry (swipe right).
     * Current view goes right, new view comes from left.
     */
    fun animateToPrevEntry(
        currentView: View,
        prevView: View,
        onComplete: () -> Unit
    ) {
        if (isAnimating) return
        isAnimating = true

        val screenWidth = rootView.width.toFloat()

        // Position previous view to the left
        prevView.translationX = -screenWidth
        prevView.visibility = View.VISIBLE

        // Animate current view out to the right
        currentView.animate()
            .translationX(screenWidth)
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(null)

        // Animate previous view in from the left
        prevView.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentView.visibility = View.GONE
                    currentView.translationX = 0f
                    currentView.alpha = 1f
                    prevView.translationX = 0f
                    isAnimating = false
                    onComplete()
                }
            })
    }

    /**
     * Animates closing the stories view (swipe down).
     * View goes down and fades out.
     */
    fun animateClose(view: View, onComplete: () -> Unit) {
        if (isAnimating) return
        isAnimating = true

        val screenHeight = rootView.height.toFloat()

        view.animate()
            .translationY(screenHeight)
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    onComplete()
                }
            })
    }

    /**
     * Animates opening the stories view (activity start).
     * View comes from bottom and fades in.
     */
    fun animateOpen(view: View, onComplete: () -> Unit) {
        val screenHeight = rootView.height.toFloat()
        
        // Check if view is measured
        if (screenHeight <= 0) {
            // View not measured yet, skip animation
            view.translationY = 0f
            view.alpha = 1f
            onComplete()
            return
        }

        // Start position: below screen
        view.translationY = screenHeight
        view.alpha = 0f

        // Animate to visible position
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.translationY = 0f
                    onComplete()
                }
            })
    }

    /**
     * Resets all animations and view states.
     */
    fun reset() {
        isAnimating = false
    }
}

