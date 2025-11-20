package ai.accelera.library.banners

/**
 * Controller for managing navigation between story cards and entries.
 */
class StoryNavigationController(
    private var viewState: StoryViewState,
    private val onNavigateToCard: (Int) -> Unit,
    private val onNavigateToNextEntry: () -> Unit,
    private val onNavigateToPrevEntry: () -> Unit,
    private val onFinish: () -> Unit
) {
    /**
     * Updates the current view state.
     */
    fun updateState(newState: StoryViewState) {
        viewState = newState
    }

    /**
     * Navigates to the next card or entry.
     */
    fun navigateToNextCard() {
        // Get current state snapshot to avoid race conditions
        val currentState = viewState
        
        when {
            currentState.isLastCard || currentState.isSingleCard -> {
                // Last card or only one card - move to next entry
                // Double-check that we're actually on the last card of the last entry before closing
                if (currentState.hasNextEntry && 
                    currentState.currentEntryIndex + 1 < currentState.entryIds.size) {
                    onNavigateToNextEntry()
                } else {
                    // Only close if we're truly on the last card of the last entry
                    // Verify we're not in an inconsistent state
                    if (currentState.currentCards.isNotEmpty() && 
                        currentState.currentCardIndex == currentState.currentCards.size - 1 &&
                        !currentState.hasNextEntry) {
                        onFinish()
                    }
                }
            }
            currentState.hasNextCard -> {
                // Move to next card
                onNavigateToCard(currentState.currentCardIndex + 1)
            }
            else -> {
                // No next card, try next entry
                // Double-check entry availability before closing
                if (currentState.hasNextEntry && 
                    currentState.currentEntryIndex + 1 < currentState.entryIds.size) {
                    onNavigateToNextEntry()
                } else {
                    // Only close if we're truly at the end
                    if (currentState.currentCards.isNotEmpty() && 
                        currentState.currentCardIndex >= currentState.currentCards.size - 1 &&
                        !currentState.hasNextEntry) {
                        onFinish()
                    }
                }
            }
        }
    }

    /**
     * Navigates to the previous card or entry.
     */
    fun navigateToPrevCard() {
        when {
            viewState.isFirstCard -> {
                // First card - move to previous entry
                if (viewState.hasPrevEntry) {
                    onNavigateToPrevEntry()
                }
            }
            viewState.hasPrevCard -> {
                // Move to previous card
                onNavigateToCard(viewState.currentCardIndex - 1)
            }
            else -> {
                // No previous card, try previous entry
                if (viewState.hasPrevEntry) {
                    onNavigateToPrevEntry()
                }
            }
        }
    }

    /**
     * Navigates to a specific card index.
     */
    fun navigateToCard(index: Int) {
        if (index < 0) {
            navigateToPrevCard()
        } else if (index >= viewState.currentCards.size) {
            navigateToNextCard()
        } else {
            onNavigateToCard(index)
        }
    }
}

