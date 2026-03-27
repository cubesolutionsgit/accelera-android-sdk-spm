package ai.accelera.library.banners.presentation.playback

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

class EntryViewRepository(
    private val context: Context,
    private val rootLayout: FrameLayout
) {
    private val entryContainers = mutableMapOf<String, StoryCardContainerView>()
    private var currentEntryId: String? = null

    fun setupEntry(
        entryId: String,
        cards: List<JSONObject>,
        jsonData: ByteArray,
        makeDivView: () -> Div2View
    ): StoryCardContainerView {
        val container = entryContainers[entryId] ?: createContainer().also {
            entryContainers[entryId] = it
        }
        container.setupCards(cards, entryId, jsonData, makeDivView)
        return container
    }

    fun getContainer(entryId: String): StoryCardContainerView? = entryContainers[entryId]

    fun setCurrentEntry(entryId: String) {
        currentEntryId = entryId
        entryContainers.forEach { (id, container) ->
            container.visibility = if (id == entryId) View.VISIBLE else View.GONE
        }
    }

    fun getCurrentEntryId(): String? = currentEntryId

    fun getCurrentContainer(): StoryCardContainerView? {
        val id = currentEntryId ?: return null
        return entryContainers[id]
    }

    fun allContainers(): Collection<StoryCardContainerView> = entryContainers.values

    fun cleanupToAdjacent(entryIds: List<String>, currentEntryIndex: Int) {
        val toKeep = mutableSetOf<String>()
        if (currentEntryIndex in entryIds.indices) toKeep.add(entryIds[currentEntryIndex])
        if (currentEntryIndex - 1 in entryIds.indices) toKeep.add(entryIds[currentEntryIndex - 1])
        if (currentEntryIndex + 1 in entryIds.indices) toKeep.add(entryIds[currentEntryIndex + 1])

        val removableIds = entryContainers.keys.filter { it !in toKeep }
        removableIds.forEach { removeEntry(it) }
    }

    fun removeEntry(entryId: String) {
        val container = entryContainers.remove(entryId) ?: return
        rootLayout.removeView(container)
        container.cleanup()
    }

    fun cleanupAll() {
        entryContainers.values.forEach { container ->
            rootLayout.removeView(container)
            container.cleanup()
        }
        entryContainers.clear()
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

