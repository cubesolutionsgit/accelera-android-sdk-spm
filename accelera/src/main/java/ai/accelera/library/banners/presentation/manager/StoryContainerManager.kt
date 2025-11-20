package ai.accelera.library.banners.presentation.manager

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView

/**
 * Manages lifecycle of story entry containers.
 * Handles creation, caching, and cleanup of StoryCardContainerView instances.
 * 
 * Follows Single Responsibility Principle - only handles container management.
 */
class StoryContainerManager(
    private val context: Context,
    private val rootLayout: FrameLayout
) {
    private val entryContainers = mutableMapOf<String, StoryCardContainerView>()
    private var currentCardContainer: StoryCardContainerView? = null

    /**
     * Gets or creates a container for the specified entry ID.
     * 
     * @param entryId The entry ID to get container for
     * @param currentEntryId The currently active entry ID (to reuse its container)
     * @return The container for the entry
     */
    fun getOrCreateContainer(
        entryId: String,
        currentEntryId: String
    ): StoryCardContainerView {
        // Check if container already exists
        entryContainers[entryId]?.let { return it }
        
        // Create or reuse container
        val container = if (entryId == currentEntryId && currentCardContainer != null) {
            // Reuse current container
            currentCardContainer!!
        } else {
            // Create new container for preloaded entry
            StoryCardContainerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                visibility = View.GONE
                // Ensure it doesn't intercept touch events
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                rootLayout.addView(this)
            }
        }
        
        // Add to containers map
        entryContainers[entryId] = container
        
        return container
    }

    /**
     * Sets the current active container.
     */
    fun setCurrentContainer(container: StoryCardContainerView) {
        currentCardContainer = container
    }

    /**
     * Gets the current active container.
     */
    fun getCurrentContainer(): StoryCardContainerView? = currentCardContainer

    /**
     * Cleans up containers that are no longer needed.
     * Keeps only current and adjacent entries (prev, current, next).
     * 
     * @param currentEntryId The currently active entry ID
     * @param currentEntryIndex The currently active entry index
     * @param entryIds List of all entry IDs
     */
    fun cleanupOldContainers(
        currentEntryId: String,
        currentEntryIndex: Int,
        entryIds: List<String>
    ) {
        // Keep only current and adjacent entries
        val entriesToKeep = mutableSetOf<String>()
        if (currentEntryIndex >= 0 && currentEntryIndex < entryIds.size) {
            entriesToKeep.add(entryIds[currentEntryIndex])
        }
        if (currentEntryIndex > 0) {
            entriesToKeep.add(entryIds[currentEntryIndex - 1])
        }
        if (currentEntryIndex >= 0 && currentEntryIndex + 1 < entryIds.size) {
            entriesToKeep.add(entryIds[currentEntryIndex + 1])
        }

        // Remove containers that are not needed
        entryContainers.keys.toList().forEach { entryId ->
            if (!entriesToKeep.contains(entryId) && entryId != currentEntryId) {
                val container = entryContainers.remove(entryId)
                container?.let {
                    if (it != currentCardContainer) {
                        rootLayout.removeView(it)
                        it.cleanup()
                    }
                }
            }
        }
    }

    /**
     * Cleans up all containers.
     * Should be called when Activity is destroyed.
     */
    fun cleanup() {
        entryContainers.values.forEach { container ->
            rootLayout.removeView(container)
            container.cleanup()
        }
        entryContainers.clear()
        currentCardContainer = null
    }
}

