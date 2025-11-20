package ai.accelera.library.banners

import android.content.Context
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
        val entriesToKeep = entriesToLoad.toSet()
        cache.keys.toList().forEach { cachedEntryId ->
            if (!entriesToKeep.contains(cachedEntryId)) {
                releaseEntry(cachedEntryId)
            }
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
        cache.remove(entryId)
    }

    /**
     * Clears all cached entries.
     */
    fun clearCache() {
        cache.clear()
        currentEntryId = ""
    }

    /**
     * Gets the current entry ID.
     */
    fun getCurrentEntryId(): String = currentEntryId
}

