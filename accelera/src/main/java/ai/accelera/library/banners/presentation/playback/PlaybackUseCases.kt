package ai.accelera.library.banners.presentation.playback

import android.os.Handler
import ai.accelera.library.Accelera
import ai.accelera.library.banners.data.repository.StoryDataRepository
import org.json.JSONObject

/**
 * Data needed to render a concrete entry.
 */
data class LoadedEntry(
    val entryId: String,
    val entryIndex: Int,
    val cards: List<JSONObject>
)

/**
 * Contract for loading a full entry payload.
 */
interface LoadEntryUseCase {
    fun load(entryId: String, entryIds: List<String>): LoadedEntry?
}

/**
 * Contract for preparing first-card video data for faster transition.
 */
interface PrepareFirstVideoUseCase {
    fun prepare(entryId: String, cards: List<JSONObject>)
}

/**
 * Contract for non-blocking adjacent preload.
 */
interface PreloadAdjacentEntriesUseCase {
    fun schedule(entryIds: List<String>, currentEntryIndex: Int)
    fun clear()
}

/**
 * Default entry loading implementation backed by [StoryDataRepository].
 */
class DefaultLoadEntryUseCase(
    private val dataRepository: StoryDataRepository
) : LoadEntryUseCase {
    override fun load(entryId: String, entryIds: List<String>): LoadedEntry? {
        val cards = dataRepository.loadEntryCards(entryId) ?: return null
        val entryIndex = entryIds.indexOf(entryId).takeIf { it >= 0 } ?: return null
        return LoadedEntry(entryId = entryId, entryIndex = entryIndex, cards = cards)
    }
}

/**
 * Prepares only the first card for each neighbor entry.
 */
class DefaultPrepareFirstVideoUseCase(
    private val repository: EntryViewRepository,
    private val jsonData: ByteArray,
    private val makeDivView: () -> com.yandex.div.core.view2.Div2View
) : PrepareFirstVideoUseCase {
    override fun prepare(entryId: String, cards: List<JSONObject>) {
        val firstCard = cards.firstOrNull() ?: return
        runCatching {
            repository.preloadFirstCard(entryId, firstCard, jsonData, makeDivView)
        }.onFailure { error ->
            Accelera.shared.error("First video preload failed for $entryId: ${error.message}")
        }
    }
}

/**
 * Schedules best-effort preload for previous and next entry only.
 */
class DefaultPreloadAdjacentEntriesUseCase(
    private val handler: Handler,
    private val dataRepository: StoryDataRepository,
    private val prepareFirstVideoUseCase: PrepareFirstVideoUseCase
) : PreloadAdjacentEntriesUseCase {
    private val queuedEntryIds = mutableSetOf<String>()
    private val runningEntryIds = mutableSetOf<String>()

    override fun schedule(entryIds: List<String>, currentEntryIndex: Int) {
        val neighbors = buildList {
            if (currentEntryIndex - 1 in entryIds.indices) add(entryIds[currentEntryIndex - 1])
            if (currentEntryIndex + 1 in entryIds.indices) add(entryIds[currentEntryIndex + 1])
        }
        neighbors.forEach { entryId ->
            if (!queuedEntryIds.add(entryId)) return@forEach
            handler.post {
                queuedEntryIds.remove(entryId)
                if (!runningEntryIds.add(entryId)) return@post
                try {
                    val cards = dataRepository.loadEntryCards(entryId) ?: return@post
                    prepareFirstVideoUseCase.prepare(entryId, cards)
                } finally {
                    runningEntryIds.remove(entryId)
                }
            }
        }
    }

    override fun clear() {
        queuedEntryIds.clear()
        runningEntryIds.clear()
        handler.removeCallbacksAndMessages(null)
    }
}

