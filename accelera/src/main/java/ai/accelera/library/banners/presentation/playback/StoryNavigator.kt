package ai.accelera.library.banners.presentation.playback

import ai.accelera.library.banners.domain.model.StoryViewState

class StoryNavigator {
    sealed class CardMove {
        data class ShowCard(val index: Int) : CardMove()
        data class MoveToEntry(val entryId: String, val toPrev: Boolean) : CardMove()
        object Close : CardMove()
        object Noop : CardMove()
    }

    fun nextByTap(state: StoryViewState): CardMove {
        return when {
            state.currentCards.isEmpty() -> CardMove.Noop
            state.currentCardIndex + 1 < state.currentCards.size -> CardMove.ShowCard(state.currentCardIndex + 1)
            state.currentEntryIndex + 1 < state.entryIds.size -> CardMove.MoveToEntry(state.entryIds[state.currentEntryIndex + 1], toPrev = false)
            else -> CardMove.Close
        }
    }

    fun prevByTap(state: StoryViewState): CardMove {
        return when {
            state.currentCards.isEmpty() -> CardMove.Noop
            state.currentCardIndex - 1 >= 0 -> CardMove.ShowCard(state.currentCardIndex - 1)
            state.currentEntryIndex - 1 >= 0 -> CardMove.MoveToEntry(state.entryIds[state.currentEntryIndex - 1], toPrev = true)
            else -> CardMove.Noop
        }
    }

    fun nextEntryBySwipe(state: StoryViewState): CardMove {
        return if (state.currentEntryIndex + 1 < state.entryIds.size) {
            CardMove.MoveToEntry(state.entryIds[state.currentEntryIndex + 1], toPrev = false)
        } else {
            CardMove.Close
        }
    }

    fun prevEntryBySwipe(state: StoryViewState): CardMove {
        return if (state.currentEntryIndex - 1 >= 0) {
            CardMove.MoveToEntry(state.entryIds[state.currentEntryIndex - 1], toPrev = true)
        } else {
            CardMove.Close
        }
    }
}

