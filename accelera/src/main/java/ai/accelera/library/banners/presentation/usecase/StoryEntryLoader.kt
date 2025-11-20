package ai.accelera.library.banners.presentation.usecase

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.model.StoryViewState
import ai.accelera.library.banners.data.repository.StoryDataRepository
import ai.accelera.library.banners.infrastructure.cache.StoryEntryPreloader
import ai.accelera.library.banners.presentation.manager.StoryContainerManager
import ai.accelera.library.banners.presentation.manager.StoryProgressManager
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import androidx.lifecycle.LifecycleOwner
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

/**
 * Handles loading and setup of story entries.
 * Coordinates between data repository, preloader, and container manager.
 * 
 * Follows Single Responsibility Principle - only handles entry loading and setup.
 */
class StoryEntryLoader(
    private val dataRepository: StoryDataRepository,
    private val entryPreloader: StoryEntryPreloader,
    private val containerManager: StoryContainerManager,
    private val progressManager: StoryProgressManager,
    private val jsonData: ByteArray,
    private val context: android.content.Context,
    private val lifecycleOwner: LifecycleOwner?
) {
    /**
     * Result of entry loading operation.
     */
    sealed class LoadResult {
        data class Success(val viewState: StoryViewState) : LoadResult()
        object Failed : LoadResult()
    }

    /**
     * Loads an entry and sets up its container.
     * 
     * @param entryId The entry ID to load
     * @param viewState Current view state
     * @param isInitialLoad Whether this is the initial load (no transition)
     * @return LoadResult with updated viewState or failure
     */
    fun loadEntry(
        entryId: String,
        viewState: StoryViewState,
        isInitialLoad: Boolean = false
    ): LoadResult {
        try {
            // Try to get cached cards first
            val cards = entryPreloader.getCachedCards(entryId)
                ?: dataRepository.loadEntryCards(entryId)
                ?: return LoadResult.Failed

            val entryIndex = viewState.entryIds.indexOf(entryId).coerceAtLeast(0)
            val cardIndex = 0

            // Update view state
            val updatedState = viewState.copy(
                currentEntryId = entryId,
                currentEntryIndex = entryIndex,
                currentCards = cards,
                currentCardIndex = cardIndex
            )

            // Preload adjacent entries
            entryPreloader.preloadAdjacentEntries(viewState.entryIds, entryIndex)

            // Setup progress bars
            progressManager.setupProgressBars(cards) { card ->
                dataRepository.hasDuration(card)
            }

            // Setup card container
            setupCardContainer(entryId, cards, updatedState.currentEntryId)

            return LoadResult.Success(updatedState)
        } catch (e: Exception) {
            Accelera.shared.error("Error in loadEntry: ${e.message}")
            return LoadResult.Failed
        }
    }

    /**
     * Sets up the card container with cards.
     * Uses cached views from preloader if available.
     */
    private fun setupCardContainer(
        entryId: String,
        cards: List<JSONObject>,
        currentEntryId: String
    ) {
        // Get or create container for this entry
        val container = containerManager.getOrCreateContainer(entryId, currentEntryId)

        // Setup cards using cached views if available
        val cachedViews = entryPreloader.getCachedCardViews(entryId)
        if (cachedViews != null && cachedViews.size == cards.size) {
            // Use cached views
            container.setupCardsWithViews(
                cards = cards,
                entryId = entryId,
                jsonData = jsonData,
                cachedViews = cachedViews
            )
        } else {
            // Create new views
            container.setupCards(
                cards = cards,
                entryId = entryId,
                jsonData = jsonData,
                makeDivView = {
                    DivKitSetup.makeView(context, jsonData, lifecycleOwner)
                }
            )
        }

        // Update current container reference
        if (entryId == currentEntryId) {
            containerManager.setCurrentContainer(container)
            // Ensure container is visible for current entry
            container.visibility = android.view.View.VISIBLE
        }
    }
}

