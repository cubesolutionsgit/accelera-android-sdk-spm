package ai.accelera.library.banners.infrastructure.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView

/**
 * Handles animations for story entry transitions.
 * Works with preloaded StoryCardContainerView instances for smooth transitions.
 */
class StoryEntryAnimator(
    private val rootView: ViewGroup,
    private val animationDuration: Long = 300L
) {
    private var isAnimating = false

    /**
     * Animates transition to next entry (swipe left).
     * Current container goes left, new container comes from right.
     * Works with preloaded StoryCardContainerView instances.
     */
    fun animateToNextEntry(
        currentContainer: StoryCardContainerView,
        nextContainer: StoryCardContainerView,
        onComplete: () -> Unit
    ) {
        if (isAnimating) return
        isAnimating = true

        val screenWidth = rootView.width.toFloat()

        // Position next container to the right
        nextContainer.translationX = screenWidth
        nextContainer.visibility = View.VISIBLE
        nextContainer.alpha = 1f

        // Animate current container out to the left
        currentContainer.animate()
            .translationX(-screenWidth)
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(null)

        // Animate next container in from the right
        nextContainer.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentContainer.visibility = View.GONE
                    currentContainer.translationX = 0f
                    currentContainer.alpha = 1f
                    // Ensure old container doesn't intercept events
                    currentContainer.isClickable = false
                    currentContainer.isFocusable = false
                    nextContainer.translationX = 0f
                    // Ensure new container is ready to receive events (but won't intercept)
                    nextContainer.isClickable = false
                    nextContainer.isFocusable = false
                    isAnimating = false
                    onComplete()
                }
            })
    }

    /**
     * Animates transition to previous entry (swipe right).
     * Current container goes right, new container comes from left.
     * Works with preloaded StoryCardContainerView instances.
     */
    fun animateToPrevEntry(
        currentContainer: StoryCardContainerView,
        prevContainer: StoryCardContainerView,
        onComplete: () -> Unit
    ) {
        if (isAnimating) return
        isAnimating = true

        val screenWidth = rootView.width.toFloat()

        // Position previous container to the left
        prevContainer.translationX = -screenWidth
        prevContainer.visibility = View.VISIBLE
        prevContainer.alpha = 1f

        // Animate current container out to the right
        currentContainer.animate()
            .translationX(screenWidth)
            .alpha(0f)
            .setDuration(animationDuration)
            .setListener(null)

        // Animate previous container in from the left
        prevContainer.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(animationDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentContainer.visibility = View.GONE
                    currentContainer.translationX = 0f
                    currentContainer.alpha = 1f
                    prevContainer.translationX = 0f
                    isAnimating = false
                    onComplete()
                }
            })
    }

    /**
     * Legacy method for backward compatibility with View.
     * Animates transition to next entry (swipe left).
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
     * Legacy method for backward compatibility with View.
     * Animates transition to previous entry (swipe right).
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
        cancelAllAnimations()
        isAnimating = false
    }
    
    /**
     * Cancels all active animations on all child views.
     * This prevents memory leaks when Activity is destroyed.
     */
    fun cancelAllAnimations() {
        try {
            // Cancel animations on all child views recursively
            cancelAnimationsRecursive(rootView)
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    /**
     * Recursively cancels animations on a view and its children.
     */
    private fun cancelAnimationsRecursive(view: View) {
        try {
            // Cancel animations on this view
            view.clearAnimation()
            view.animate()?.cancel()
            
            // Recursively cancel animations on children
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    cancelAnimationsRecursive(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}

