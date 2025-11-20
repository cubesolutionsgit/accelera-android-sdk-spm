package ai.accelera.library.banners.domain.usecase

import ai.accelera.library.banners.domain.model.StoryViewState

/**
 * Use case for story navigation logic.
 * Contains business logic for navigating between cards and entries.
 * 
 * Follows Single Responsibility Principle - only handles navigation logic.
 * Follows Dependency Inversion Principle - depends on abstractions (StoryViewState).
 */
class StoryNavigationUseCase {

    /**
     * Result of navigation action.
     */
    sealed class NavigationResult {
        data class NavigateToCard(val index: Int) : NavigationResult()
        data class NavigateToNextEntry(val entryId: String) : NavigationResult()
        data class NavigateToPrevEntry(val entryId: String) : NavigationResult()
        object Finish : NavigationResult()
        object NoAction : NavigationResult()
    }

    /**
     * Navigates to the next card or entry.
     */
    fun navigateToNext(state: StoryViewState): NavigationResult {
        return when {
            state.isLastCard || state.isSingleCard -> {
                // Last card or only one card - move to next entry
                if (state.hasNextEntry && state.currentEntryIndex + 1 < state.entryIds.size) {
                    val nextEntryId = state.entryIds[state.currentEntryIndex + 1]
                    NavigationResult.NavigateToNextEntry(nextEntryId)
                } else {
                    // Last card of last entry - finish
                    NavigationResult.Finish
                }
            }
            state.hasNextCard -> {
                // Move to next card
                NavigationResult.NavigateToCard(state.currentCardIndex + 1)
            }
            else -> {
                // No next card, try next entry
                if (state.hasNextEntry && state.currentEntryIndex + 1 < state.entryIds.size) {
                    val nextEntryId = state.entryIds[state.currentEntryIndex + 1]
                    NavigationResult.NavigateToNextEntry(nextEntryId)
                } else {
                    NavigationResult.Finish
                }
            }
        }
    }

    /**
     * Navigates to the previous card or entry.
     */
    fun navigateToPrev(state: StoryViewState): NavigationResult {
        return when {
            state.isFirstCard -> {
                // First card - move to previous entry
                if (state.hasPrevEntry) {
                    val prevEntryId = state.entryIds[state.currentEntryIndex - 1]
                    NavigationResult.NavigateToPrevEntry(prevEntryId)
                } else {
                    NavigationResult.NoAction
                }
            }
            state.hasPrevCard -> {
                // Move to previous card
                NavigationResult.NavigateToCard(state.currentCardIndex - 1)
            }
            else -> {
                // No previous card, try previous entry
                if (state.hasPrevEntry) {
                    val prevEntryId = state.entryIds[state.currentEntryIndex - 1]
                    NavigationResult.NavigateToPrevEntry(prevEntryId)
                } else {
                    NavigationResult.NoAction
                }
            }
        }
    }

    /**
     * Navigates to a specific card index.
     * Validates the index and handles boundary cases.
     */
    fun navigateToCard(state: StoryViewState, index: Int): NavigationResult {
        return when {
            index < 0 -> {
                // Try to go to previous entry
                if (state.hasPrevEntry) {
                    val prevEntryId = state.entryIds[state.currentEntryIndex - 1]
                    NavigationResult.NavigateToPrevEntry(prevEntryId)
                } else {
                    NavigationResult.NoAction
                }
            }
            index >= state.currentCards.size -> {
                // Try to go to next entry
                if (state.hasNextEntry && state.currentEntryIndex + 1 < state.entryIds.size) {
                    val nextEntryId = state.entryIds[state.currentEntryIndex + 1]
                    NavigationResult.NavigateToNextEntry(nextEntryId)
                } else {
                    NavigationResult.Finish
                }
            }
            else -> {
                // Valid card index
                NavigationResult.NavigateToCard(index)
            }
        }
    }

    /**
     * Validates if navigation to next is possible.
     */
    fun canNavigateNext(state: StoryViewState): Boolean {
        return state.hasNextCard || state.hasNextEntry
    }

    /**
     * Validates if navigation to previous is possible.
     */
    fun canNavigatePrev(state: StoryViewState): Boolean {
        return state.hasPrevCard || state.hasPrevEntry
    }
}
