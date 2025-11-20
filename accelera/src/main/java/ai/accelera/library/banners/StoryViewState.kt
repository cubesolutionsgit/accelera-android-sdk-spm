package ai.accelera.library.banners

import org.json.JSONObject

/**
 * Data class representing the current state of stories view.
 */
data class StoryViewState(
    val currentEntryId: String = "",
    val currentEntryIndex: Int = 0,
    val entryIds: List<String> = emptyList(),
    val currentCards: List<JSONObject> = emptyList(),
    val currentCardIndex: Int = 0
) {
    val hasNextCard: Boolean
        get() = currentCardIndex + 1 < currentCards.size

    val hasPrevCard: Boolean
        get() = currentCardIndex > 0

    val hasNextEntry: Boolean
        get() = currentEntryIndex + 1 < entryIds.size

    val hasPrevEntry: Boolean
        get() = currentEntryIndex > 0

    val isFirstCard: Boolean
        get() = currentCardIndex == 0

    val isLastCard: Boolean
        get() = currentCardIndex == currentCards.size - 1

    val isSingleCard: Boolean
        get() = currentCards.size == 1
}

