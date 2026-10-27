package ai.accelera.library.banners

import ai.accelera.library.banners.data.repository.StoryDataRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupDestinationPolicyTest {

    @Test
    fun emptyEntryIds_resolvesToPopup() {
        assertEquals(PopupDestination.Popup, PopupDestinationPolicy.resolve(emptyList()))
    }

    @Test
    fun firstEntryId_resolvesToFullscreen() {
        assertEquals(
            PopupDestination.Fullscreen("001_fs_121"),
            PopupDestinationPolicy.resolve(listOf("001_fs_121", "002_fs_122"))
        )
    }

    @Test
    fun singleStoryPayload_opensFirstFullscreenEntry() {
        val json = """{"fullscreens":{"002":{"cards":[]},"001_fs_121":{"cards":[{"card":{}}]}}}"""
        val ids = StoryDataRepository(json.toByteArray()).loadEntryIds()
        assertEquals(PopupDestination.Fullscreen("001_fs_121"), PopupDestinationPolicy.resolve(ids))
    }

    @Test
    fun plainCardPayload_staysPopup() {
        val json = """{"card":{"log_id":"popup","states":[]}}"""
        val ids = StoryDataRepository(json.toByteArray()).loadEntryIds()
        assertEquals(PopupDestination.Popup, PopupDestinationPolicy.resolve(ids))
    }
}
