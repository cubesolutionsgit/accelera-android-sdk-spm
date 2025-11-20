package ai.accelera.library.banners

import android.content.Context
import android.view.ViewGroup
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

/**
 * Manages preloading and caching of story entries.
 * Preloads adjacent entries (previous, current, next) to ensure smooth transitions.
 * 
 * Follows Single Responsibility Principle - only handles preloading and caching.
 */
class StoryEntryPreloader(
    private val context: Context,
    private val jsonData: ByteArray,
    private val dataRepository: StoryDataRepository,
    private val makeDivView: () -> Div2View
) {
    /**
     * Cache entry for storing preloaded cards and their views.
     */
    private data class EntryCache(
        val entryId: String,
        val cards: List<JSONObject>,
        val cardViews: MutableMap<Int, Div2View> = mutableMapOf()
    )

    private val cache = mutableMapOf<String, EntryCache>()
    private var currentEntryId: String = ""

    /**
     * Preloads adjacent entries (previous, current, next).
     */
    fun preloadAdjacentEntries(
        entryIds: List<String>,
        currentEntryIndex: Int
    ) {
        val entriesToLoad = mutableSetOf<String>()
        
        // Current entry
        if (currentEntryIndex >= 0 && currentEntryIndex < entryIds.size) {
            entriesToLoad.add(entryIds[currentEntryIndex])
        }
        
        // Previous entry
        if (currentEntryIndex > 0) {
            entriesToLoad.add(entryIds[currentEntryIndex - 1])
        }
        
        // Next entry
        if (currentEntryIndex >= 0 && currentEntryIndex + 1 < entryIds.size) {
            entriesToLoad.add(entryIds[currentEntryIndex + 1])
        }
        
        // Load entries
        entriesToLoad.forEach { entryId ->
            if (!cache.containsKey(entryId)) {
                loadEntry(entryId)
            }
        }
        
        // Clean up entries that are too far away (keep only current + 2 adjacent)
        // Also limit cache size to prevent memory issues (max 5 entries)
        val entriesToKeep = entriesToLoad.toSet()
        val entriesToRemove = mutableListOf<String>()
        
        cache.keys.forEach { cachedEntryId ->
            if (!entriesToKeep.contains(cachedEntryId)) {
                entriesToRemove.add(cachedEntryId)
            }
        }
        
        // Aggressively limit cache size to prevent memory issues (max 3 entries)
        if (cache.size > 3) {
            val sortedKeys = cache.keys.toList()
            val excessCount = cache.size - 3
            for (i in 0 until excessCount) {
                if (i < sortedKeys.size && !entriesToKeep.contains(sortedKeys[i])) {
                    entriesToRemove.add(sortedKeys[i])
                }
            }
        }
        
        entriesToRemove.forEach { entryId ->
            releaseEntry(entryId)
        }
        
        currentEntryId = if (currentEntryIndex >= 0 && currentEntryIndex < entryIds.size) {
            entryIds[currentEntryIndex]
        } else {
            ""
        }
    }

    /**
     * Loads an entry and caches its cards.
     */
    private fun loadEntry(entryId: String) {
        val cards = dataRepository.loadEntryCards(entryId) ?: return
        
        val entryCache = EntryCache(entryId, cards)
        
        // Pre-create Div2View instances for all cards
        cards.forEachIndexed { index, card ->
            val divView = makeDivView()
            val cardBytes = card.toString().toByteArray(Charsets.UTF_8)
            val divData = DivKitSetup.parseDivData(cardBytes)
            if (divData != null) {
                val tag = com.yandex.div.DivDataTag("story_${entryId}_$index")
                divView.setData(divData, tag)
            }
            entryCache.cardViews[index] = divView
        }
        
        cache[entryId] = entryCache
    }

    /**
     * Gets cached cards for an entry.
     */
    fun getCachedCards(entryId: String): List<JSONObject>? {
        return cache[entryId]?.cards
    }

    /**
     * Gets cached Div2View for a specific card.
     */
    fun getCachedCardView(entryId: String, cardIndex: Int): Div2View? {
        return cache[entryId]?.cardViews?.get(cardIndex)
    }

    /**
     * Gets all cached card views for an entry.
     */
    fun getCachedCardViews(entryId: String): Map<Int, Div2View>? {
        return cache[entryId]?.cardViews
    }

    /**
     * Checks if an entry is cached.
     */
    fun isCached(entryId: String): Boolean {
        return cache.containsKey(entryId)
    }

    /**
     * Releases resources for a specific entry.
     */
    fun releaseEntry(entryId: String) {
        val entryCache = cache[entryId] ?: return
        
        // Release all Div2View resources before removing from cache
        entryCache.cardViews.values.forEach { divView ->
            try {
                // Cancel any animations
                divView.clearAnimation()
                divView.animate()?.cancel()
                
                // Remove from parent if attached
                // This will automatically trigger resource cleanup in Div2View
                (divView.parent as? ViewGroup)?.removeView(divView)
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }
        
        // Clear cardViews map
        entryCache.cardViews.clear()
        
        // Remove from cache
        cache.remove(entryId)
    }

    /**
     * Clears all cached entries.
     */
    fun clearCache() {
        // Release all entries before clearing
        cache.keys.toList().forEach { entryId ->
            releaseEntry(entryId)
        }
        cache.clear()
        currentEntryId = ""
    }

    /**
     * Gets the current entry ID.
     */
    fun getCurrentEntryId(): String = currentEntryId
}

