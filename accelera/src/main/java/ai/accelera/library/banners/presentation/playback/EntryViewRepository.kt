package ai.accelera.library.banners.presentation.playback

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import com.yandex.div.DivDataTag
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

/**
 * Stores and manages Story entry containers and lightweight preloaded first-card views.
 */
class EntryViewRepository(
    private val context: Context,
    private val rootLayout: FrameLayout
) {
    private val entryContainers = mutableMapOf<String, StoryCardContainerView>()
    private val preloadedFirstCardViews = mutableMapOf<String, Div2View>()
    private var currentEntryId: String? = null

    /**
     * Builds or updates an entry container with full card list.
     */
    fun setupEntry(
        entryId: String,
        cards: List<JSONObject>,
        jsonData: ByteArray,
        makeDivView: () -> Div2View
    ): StoryCardContainerView {
        val container = entryContainers[entryId] ?: createContainer().also {
            entryContainers[entryId] = it
        }
        val preloaded = preloadedFirstCardViews.remove(entryId)
        val cachedViews = if (preloaded != null) mapOf(0 to preloaded) else emptyMap()
        if (cachedViews.isNotEmpty()) {
            container.setupCardsWithViews(
                cards = cards,
                entryId = entryId,
                jsonData = jsonData,
                cachedViews = cachedViews
            )
        } else {
            container.setupCards(cards, entryId, jsonData, makeDivView)
        }
        return container
    }

    /**
     * Prepares the first card view for a neighbor entry for faster next transition.
     */
    fun preloadFirstCard(
        entryId: String,
        firstCard: JSONObject,
        jsonData: ByteArray,
        makeDivView: () -> Div2View
    ) {
        if (preloadedFirstCardViews.containsKey(entryId)) return
        val divView = makeDivView()
        val cardBytes = firstCard.toString().toByteArray(Charsets.UTF_8)
        val divData = DivKitSetup.parseDivData(cardBytes) ?: return
        val tag = DivDataTag("story_${entryId}_0_preload")
        divView.setData(divData, tag)
        DivKitSetup.pauseVideoPlayers(divView)
        preloadedFirstCardViews[entryId] = divView
    }

    fun getContainer(entryId: String): StoryCardContainerView? = entryContainers[entryId]

    /**
     * Marks an entry as visible and hides other entry containers.
     */
    fun setCurrentEntry(entryId: String) {
        currentEntryId = entryId
        entryContainers.forEach { (id, container) ->
            container.visibility = if (id == entryId) View.VISIBLE else View.GONE
        }
    }

    fun getCurrentEntryId(): String? = currentEntryId

    /**
     * Returns container for the current entry.
     */
    fun getCurrentContainer(): StoryCardContainerView? {
        val id = currentEntryId ?: return null
        return entryContainers[id]
    }

    fun allContainers(): Collection<StoryCardContainerView> = entryContainers.values

    /**
     * Keeps only current and adjacent entry containers in memory.
     */
    fun cleanupToAdjacent(entryIds: List<String>, currentEntryIndex: Int) {
        val toKeep = mutableSetOf<String>()
        if (currentEntryIndex in entryIds.indices) toKeep.add(entryIds[currentEntryIndex])
        if (currentEntryIndex - 1 in entryIds.indices) toKeep.add(entryIds[currentEntryIndex - 1])
        if (currentEntryIndex + 1 in entryIds.indices) toKeep.add(entryIds[currentEntryIndex + 1])

        val removableIds = entryContainers.keys.filter { it !in toKeep }
        removableIds.forEach { removeEntry(it) }
    }

    /**
     * Removes entry container and releases all associated resources.
     */
    fun removeEntry(entryId: String) {
        val container = entryContainers.remove(entryId) ?: return
        rootLayout.removeView(container)
        container.cleanup()
        preloadedFirstCardViews.remove(entryId)?.let { DivKitSetup.releaseVideoPlayers(it) }
    }

    /**
     * Releases all repository resources.
     */
    fun cleanupAll() {
        entryContainers.values.forEach { container ->
            rootLayout.removeView(container)
            container.cleanup()
        }
        preloadedFirstCardViews.values.forEach { view ->
            DivKitSetup.releaseVideoPlayers(view)
            (view.parent as? ViewGroup)?.removeView(view)
        }
        entryContainers.clear()
        preloadedFirstCardViews.clear()
        currentEntryId = null
    }

    private fun createContainer(): StoryCardContainerView {
        val container = StoryCardContainerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
        // Keep story content below overlays (progress + close button)
        rootLayout.addView(container, 0)
        return container
    }
}

