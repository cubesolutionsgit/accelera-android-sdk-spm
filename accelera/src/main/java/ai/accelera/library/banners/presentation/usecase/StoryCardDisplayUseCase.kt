package ai.accelera.library.banners.presentation.usecase

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.model.StoryViewState
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import ai.accelera.library.banners.presentation.manager.StoryProgressManager
import ai.accelera.library.banners.infrastructure.logging.StoryEventLogger
import ai.accelera.library.banners.data.repository.StoryDataRepository

/**
 * Use case for displaying a story card.
 * Coordinates card display, progress setup, and event logging.
 * 
 * Follows Single Responsibility Principle - only handles card display logic.
 */
class StoryCardDisplayUseCase(
    private val progressManager: StoryProgressManager,
    private val eventLogger: StoryEventLogger,
    private val dataRepository: StoryDataRepository
) {
    /**
     * Result of card display operation.
     */
    sealed class DisplayResult {
        object Success : DisplayResult()
        object InvalidIndex : DisplayResult()
        object EmptyCards : DisplayResult()
        object Transitioning : DisplayResult()
    }

    /**
     * Displays a card at the specified index.
     * 
     * @param viewState Current view state
     * @param cardIndex Index of card to display
     * @param container Container to display card in
     * @param animate Whether to animate the transition
     * @param isFinishing Whether Activity is finishing
     * @param isDestroyed Whether Activity is destroyed
     * @param isTransitioning Whether a transition is in progress
     * @return DisplayResult indicating success or reason for failure
     */
    fun displayCard(
        viewState: StoryViewState,
        cardIndex: Int,
        container: StoryCardContainerView,
        animate: Boolean = true,
        isFinishing: Boolean = false,
        isDestroyed: Boolean = false,
        isTransitioning: Boolean = false
    ): DisplayResult {
        // Don't process during transitions
        if (isTransitioning) {
            return DisplayResult.Transitioning
        }

        // Check if Activity is finishing or destroyed
        if (isFinishing || isDestroyed) {
            return DisplayResult.InvalidIndex
        }

        // Check if cards are available
        if (viewState.currentCards.isEmpty()) {
            Accelera.shared.error("displayCard called but currentCards is empty")
            return DisplayResult.EmptyCards
        }

        // Validate index
        if (cardIndex < 0 || cardIndex >= viewState.currentCards.size) {
            return DisplayResult.InvalidIndex
        }

        try {
            // Ensure container is visible
            container.visibility = android.view.View.VISIBLE
            
            // Show card in container
            container.showCard(cardIndex, animate = animate)

            // Log view event
            val card = viewState.currentCards[cardIndex]
            eventLogger.logCardView(card)

            // Start progress animation
            val duration = dataRepository.getCardDuration(card)
            progressManager.showCard(cardIndex, duration)

            return DisplayResult.Success
        } catch (e: Exception) {
            Accelera.shared.error("Error in displayCard: ${e.message}")
            return DisplayResult.InvalidIndex
        }
    }
}

