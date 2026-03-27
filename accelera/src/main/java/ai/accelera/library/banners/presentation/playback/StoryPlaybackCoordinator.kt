package ai.accelera.library.banners.presentation.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import ai.accelera.library.Accelera
import ai.accelera.library.banners.data.repository.StoryDataRepository
import ai.accelera.library.banners.domain.model.StoryViewState
import ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.banners.infrastructure.logging.StoryEventLogger
import ai.accelera.library.banners.presentation.manager.StoryProgressManager
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject

class StoryPlaybackCoordinator(
    private val context: Context,
    private val rootLayout: FrameLayout,
    private val progressContainer: ViewGroup,
    private val jsonData: ByteArray,
    private val lifecycleOwner: LifecycleOwner,
    private val onCloseRequested: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    private val dataRepository = StoryDataRepository(jsonData)
    private val eventLogger = StoryEventLogger(dataRepository)
    private val stateMachine = PlaybackStateMachine()
    private val navigator = StoryNavigator()
    private val repository = EntryViewRepository(context, rootLayout)
    private val playerController = PlayerLifecycleController(repository)
    private val entryAnimator = StoryEntryAnimator(rootLayout)
    private val progressManager = StoryProgressManager(
        context = context,
        progressContainer = progressContainer,
        onProgressComplete = { handleEvent(PlaybackEvent.TapNext) }
    )

    private var viewState = StoryViewState()

    fun open(entryId: String): Boolean {
        if (!stateMachine.onEvent(PlaybackEvent.Open(entryId))) return false
        val entryIds = dataRepository.loadEntryIds()
        if (entryIds.isEmpty()) return false
        val index = entryIds.indexOf(entryId).takeIf { it >= 0 } ?: 0
        val resolvedEntryId = entryIds[index]
        viewState = viewState.copy(
            entryIds = entryIds,
            currentEntryIndex = index,
            currentEntryId = resolvedEntryId
        )
        return prepareAndShowEntry(resolvedEntryId, animate = false, isPrev = false)
    }

    fun handleEvent(event: PlaybackEvent) {
        when (event) {
            PlaybackEvent.TapNext -> processMove(navigator.nextByTap(viewState))
            PlaybackEvent.TapPrev -> processMove(navigator.prevByTap(viewState))
            PlaybackEvent.SwipeNextEntry -> {
                if (!stateMachine.onEvent(event)) return
                processMove(navigator.nextEntryBySwipe(viewState))
            }

            PlaybackEvent.SwipePrevEntry -> {
                if (!stateMachine.onEvent(event)) return
                processMove(navigator.prevEntryBySwipe(viewState))
            }

            PlaybackEvent.LongPressStart -> {
                if (stateMachine.onEvent(event)) {
                    progressManager.pauseProgress()
                    playerController.pauseForLifecycle()
                }
            }

            PlaybackEvent.LongPressEnd -> {
                if (stateMachine.onEvent(event)) {
                    progressManager.resumeProgress()
                    playerController.resumeAfterLifecycle()
                }
            }

            PlaybackEvent.ActivityPause -> {
                if (stateMachine.onEvent(event)) {
                    progressManager.pauseProgress()
                    playerController.pauseForLifecycle()
                }
            }

            PlaybackEvent.ActivityResume -> {
                if (stateMachine.onEvent(event)) {
                    playerController.resumeAfterLifecycle()
                    progressManager.resumeProgress()
                }
            }

            PlaybackEvent.CloseRequested -> {
                if (stateMachine.onEvent(event)) {
                    progressManager.stopProgress()
                    playerController.pauseForLifecycle()
                    onCloseRequested()
                }
            }

            PlaybackEvent.Destroyed -> {
                stateMachine.onEvent(event)
                progressManager.cleanup()
                playerController.releaseAll()
                entryAnimator.reset()
                repository.cleanupAll()
                handler.removeCallbacksAndMessages(null)
            }

            is PlaybackEvent.Open -> Unit
        }
    }

    private fun processMove(move: StoryNavigator.CardMove) {
        when (move) {
            is StoryNavigator.CardMove.ShowCard -> showCard(move.index, animate = true)
            is StoryNavigator.CardMove.MoveToEntry -> transitionToEntry(move.entryId, move.toPrev)
            StoryNavigator.CardMove.Close -> handleEvent(PlaybackEvent.CloseRequested)
            StoryNavigator.CardMove.Noop -> Unit
        }
    }

    private fun showCard(index: Int, animate: Boolean) {
        if (index !in viewState.currentCards.indices) return

        val container = repository.getCurrentContainer() ?: return
        viewState = viewState.copy(currentCardIndex = index)

        // Fixed sequence: stop progress -> deactivate hidden -> prepare/activate -> start progress -> log event.
        progressManager.stopProgress()
        playerController.deactivateHiddenCards(viewState.currentEntryId)
        playerController.prepareVisibleCard(viewState.currentEntryId, index)
        container.showCard(index, animate)
        playerController.activateVisibleCard(viewState.currentEntryId, index)
        startProgressAndLog(index)
        stateMachine.forceState(PlaybackState.ShowingCard)
    }

    private fun transitionToEntry(entryId: String, isPrev: Boolean) {
        if (stateMachine.state !in setOf(PlaybackState.TransitioningEntry, PlaybackState.ShowingCard)) return
        val currentContainer = repository.getCurrentContainer() ?: return

        progressManager.stopProgress()
        playerController.deactivateHiddenCards(viewState.currentEntryId)

        val loaded = prepareEntry(entryId) ?: run {
            stateMachine.forceState(PlaybackState.ShowingCard)
            return
        }
        val targetContainer = loaded.container

        if (currentContainer === targetContainer) {
            onEntryTransitionComplete(loaded.entryId, targetContainer)
            return
        }

        val runAnimation = if (isPrev) entryAnimator::animateToPrevEntry else entryAnimator::animateToNextEntry
        runAnimation(currentContainer, targetContainer) {
            onEntryTransitionComplete(loaded.entryId, targetContainer)
        }
    }

    private data class PreparedEntry(
        val entryId: String,
        val cards: List<JSONObject>,
        val entryIndex: Int,
        val container: StoryCardContainerView
    )

    private fun prepareEntry(entryId: String): PreparedEntry? {
        stateMachine.forceState(PlaybackState.PreparingEntry)
        val cards = dataRepository.loadEntryCards(entryId) ?: return null
        val entryIndex = viewState.entryIds.indexOf(entryId).takeIf { it >= 0 } ?: return null
        val container = repository.setupEntry(
            entryId = entryId,
            cards = cards,
            jsonData = jsonData,
            makeDivView = { DivKitSetup.makeView(context, jsonData, lifecycleOwner) }
        )
        return PreparedEntry(entryId, cards, entryIndex, container)
    }

    private fun prepareAndShowEntry(entryId: String, animate: Boolean, isPrev: Boolean): Boolean {
        val prepared = prepareEntry(entryId) ?: return false
        repository.setCurrentEntry(prepared.entryId)
        viewState = viewState.copy(
            currentEntryId = prepared.entryId,
            currentEntryIndex = prepared.entryIndex,
            currentCards = prepared.cards,
            currentCardIndex = 0
        )
        prepared.container.visibility = android.view.View.VISIBLE
        prepared.container.showCard(0, animate)
        playerController.prepareVisibleCard(prepared.entryId, 0)
        playerController.activateVisibleCard(prepared.entryId, 0)
        startProgressAndLog(0)
        repository.cleanupToAdjacent(viewState.entryIds, viewState.currentEntryIndex)
        stateMachine.forceState(if (animate) PlaybackState.TransitioningEntry else PlaybackState.ShowingCard)
        if (animate) {
            transitionToEntry(entryId, isPrev)
        } else {
            stateMachine.forceState(PlaybackState.ShowingCard)
        }
        return true
    }

    private fun onEntryTransitionComplete(entryId: String, targetContainer: StoryCardContainerView) {
        repository.setCurrentEntry(entryId)
        val cards = dataRepository.loadEntryCards(entryId) ?: emptyList()
        val entryIndex = viewState.entryIds.indexOf(entryId).coerceAtLeast(0)
        viewState = viewState.copy(
            currentEntryId = entryId,
            currentEntryIndex = entryIndex,
            currentCards = cards,
            currentCardIndex = 0
        )
        targetContainer.visibility = android.view.View.VISIBLE
        targetContainer.showCard(0, animate = false)
        playerController.prepareVisibleCard(entryId, 0)
        playerController.activateVisibleCard(entryId, 0)
        startProgressAndLog(0)
        repository.cleanupToAdjacent(viewState.entryIds, viewState.currentEntryIndex)
        handler.postDelayed({ stateMachine.forceState(PlaybackState.ShowingCard) }, 40L)
    }

    private fun startProgressAndLog(cardIndex: Int) {
        val card = viewState.currentCards.getOrNull(cardIndex) ?: return
        eventLogger.logCardView(card)
        progressManager.setupProgressBars(viewState.currentCards) { candidate ->
            dataRepository.hasDuration(candidate)
        }
        progressManager.showCard(cardIndex, dataRepository.getCardDuration(card))
    }
}

