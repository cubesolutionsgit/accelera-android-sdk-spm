package ai.accelera.library.banners.infrastructure.cache

import ai.accelera.library.banners.domain.model.StoryPlaybackSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoryPlaybackStateStoreTest {

    @Test
    fun `saved snapshot is returned for the same token`() {
        val snapshot = StoryPlaybackSnapshot(entryId = "entry-1", cardIndex = 2, progressFraction = 0.5f)
        StoryPlaybackStateStore.save("token-a", snapshot)

        assertEquals(snapshot, StoryPlaybackStateStore.get("token-a"))

        StoryPlaybackStateStore.remove("token-a")
    }

    @Test
    fun `get returns null for unknown null or blank token`() {
        assertNull(StoryPlaybackStateStore.get("unknown-token"))
        assertNull(StoryPlaybackStateStore.get(null))
        assertNull(StoryPlaybackStateStore.get(""))
    }

    @Test
    fun `save with null or blank token is ignored`() {
        val snapshot = StoryPlaybackSnapshot(entryId = "entry-1", cardIndex = 0, progressFraction = 0f)
        StoryPlaybackStateStore.save(null, snapshot)
        StoryPlaybackStateStore.save("", snapshot)

        assertNull(StoryPlaybackStateStore.get(null))
        assertNull(StoryPlaybackStateStore.get(""))
    }

    @Test
    fun `remove deletes the snapshot`() {
        val snapshot = StoryPlaybackSnapshot(entryId = "entry-1", cardIndex = 1, progressFraction = 0.25f)
        StoryPlaybackStateStore.save("token-b", snapshot)

        StoryPlaybackStateStore.remove("token-b")

        assertNull(StoryPlaybackStateStore.get("token-b"))
    }

    @Test
    fun `later save overwrites earlier snapshot for the same token`() {
        StoryPlaybackStateStore.save("token-c", StoryPlaybackSnapshot("entry-1", 0, 0f))
        val latest = StoryPlaybackSnapshot("entry-2", 3, 0.75f)
        StoryPlaybackStateStore.save("token-c", latest)

        assertEquals(latest, StoryPlaybackStateStore.get("token-c"))

        StoryPlaybackStateStore.remove("token-c")
    }
}
