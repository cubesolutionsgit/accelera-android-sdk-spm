package ai.accelera.library.banners.presentation.playback

import ai.accelera.library.banners.domain.model.StoryViewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryNavigatorTest {
    private val navigator = StoryNavigator()

    @Test
    fun `nextByTap moves to next card inside same entry`() {
        val state = StoryViewState(
            entryIds = listOf("e1", "e2"),
            currentEntryIndex = 0,
            currentCards = fakeCards(2),
            currentCardIndex = 0
        )

        val move = navigator.nextByTap(state)
        assertTrue(move is StoryNavigator.CardMove.ShowCard)
        assertEquals(1, (move as StoryNavigator.CardMove.ShowCard).index)
    }

    @Test
    fun `nextByTap closes on last entry last card`() {
        val state = StoryViewState(
            entryIds = listOf("e1"),
            currentEntryIndex = 0,
            currentCards = fakeCards(1),
            currentCardIndex = 0
        )

        val move = navigator.nextByTap(state)
        assertTrue(move is StoryNavigator.CardMove.Close)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fakeCards(size: Int): List<org.json.JSONObject> =
        List(size) { Any() } as List<org.json.JSONObject>
}
