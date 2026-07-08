package ai.accelera.library.banners.infrastructure.cache

import ai.accelera.library.banners.domain.model.StoryPlaybackSnapshot
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process store of stories playback positions, keyed by the payload token of the
 * launching Intent. Lets a recreated FullscreenActivity (rotation, theme change)
 * resume at the same entry/card/timer position instead of restarting from scratch.
 * Entries are removed when the screen actually finishes.
 */
internal object StoryPlaybackStateStore {
    private val snapshots = ConcurrentHashMap<String, StoryPlaybackSnapshot>()

    fun save(token: String?, snapshot: StoryPlaybackSnapshot) {
        if (!token.isNullOrBlank()) {
            snapshots[token] = snapshot
        }
    }

    fun get(token: String?): StoryPlaybackSnapshot? {
        if (token.isNullOrBlank()) return null
        return snapshots[token]
    }

    fun remove(token: String?) {
        if (!token.isNullOrBlank()) {
            snapshots.remove(token)
        }
    }
}
