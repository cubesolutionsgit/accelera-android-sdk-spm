package ai.accelera.library.banners.presentation.manager

/**
 * Manages transition state and prevents rapid consecutive transitions.
 * 
 * Follows Single Responsibility Principle - only handles transition state management.
 */
class StoryTransitionManager(
    private val minTransitionDuration: Long = 200L
) {
    private var isTransitioning = false
    private var transitionStartTime = 0L

    /**
     * Checks if a transition can be started.
     * Returns true if no transition is in progress and enough time has passed since last transition.
     */
    fun canStartTransition(): Boolean {
        val currentTime = System.currentTimeMillis()
        return !isTransitioning && (currentTime - transitionStartTime >= minTransitionDuration)
    }

    /**
     * Starts a transition.
     * Should be called when beginning a transition animation.
     */
    fun startTransition() {
        isTransitioning = true
        transitionStartTime = System.currentTimeMillis()
    }

    /**
     * Completes a transition.
     * Should be called when transition animation completes.
     */
    fun completeTransition() {
        isTransitioning = false
    }

    /**
     * Checks if currently transitioning.
     */
    fun isTransitioning(): Boolean = isTransitioning

    /**
     * Resets transition state.
     * Should be called when Activity is destroyed or reset.
     */
    fun reset() {
        isTransitioning = false
        transitionStartTime = 0L
    }
}

