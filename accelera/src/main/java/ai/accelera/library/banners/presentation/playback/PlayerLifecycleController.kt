package ai.accelera.library.banners.presentation.playback

import ai.accelera.library.Accelera
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup

class PlayerLifecycleController(
    private val repository: EntryViewRepository
) {
    private var activeEntryId: String? = null
    private var activeCardIndex: Int = 0

    fun prepareVisibleCard(entryId: String, cardIndex: Int) {
        activeEntryId = entryId
        activeCardIndex = cardIndex
    }

    fun activateVisibleCard(entryId: String, cardIndex: Int, restartPlayback: Boolean = true) {
        activeEntryId = entryId
        activeCardIndex = cardIndex
        val container = repository.getContainer(entryId) ?: return
        val cardView = container.getCardView(cardIndex) ?: return
        if (restartPlayback) {
            DivKitSetup.restartVideoPlayers(cardView)
        } else {
            DivKitSetup.playVideoPlayers(cardView)
        }
        Accelera.shared.log("Stories player activate: entry=$entryId card=$cardIndex")
    }

    fun deactivateHiddenCards(activeEntryId: String) {
        repository.allContainers().forEach { container ->
            runCatching { container.pauseVideoPlayers() }
        }
        this.activeEntryId = activeEntryId
    }

    fun pauseForLifecycle() {
        repository.allContainers().forEach { container ->
            runCatching { container.pauseVideoPlayers() }
        }
        Accelera.shared.log("Stories player pauseForLifecycle")
    }

    fun resumeAfterLifecycle() {
        val entryId = activeEntryId ?: return
        val container = repository.getContainer(entryId) ?: return
        runCatching { container.showCard(activeCardIndex, animate = false, restartPlayback = false) }
        Accelera.shared.log("Stories player resumeAfterLifecycle: entry=$entryId card=$activeCardIndex")
    }

    fun releaseEntry(entryId: String) {
        repository.getContainer(entryId)?.releaseVideoPlayers()
        if (activeEntryId == entryId) {
            activeEntryId = null
            activeCardIndex = 0
        }
    }

    fun releaseAll() {
        repository.allContainers().forEach { container ->
            runCatching { container.releaseVideoPlayers() }
        }
        activeEntryId = null
        activeCardIndex = 0
    }
}

