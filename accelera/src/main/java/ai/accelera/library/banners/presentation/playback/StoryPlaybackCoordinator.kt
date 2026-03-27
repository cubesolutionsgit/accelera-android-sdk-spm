package ai.accelera.library.banners.presentation.playback

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import ai.accelera.library.banners.domain.model.StoryViewState
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import androidx.lifecycle.LifecycleOwner

/**
 * Orchestrates story playback lifecycle, navigation and preload pipeline.
 */
class StoryPlaybackCoordinator(
    private val context: Context,
    private val rootLayout: FrameLayout,
    private val progressContainer: ViewGroup,
    private val jsonData: ByteArray,
    private val lifecycleOwner: LifecycleOwner,
    private val onCloseRequested: () -> Unit,
    dependencies: StoryPlaybackDependencies? = null
) {
    private val deps = dependencies ?: StoryPlaybackDependencyFactory.create(
        context = context,
        rootLayout = rootLayout,
        progressContainer = progressContainer,
        jsonData = jsonData,
        lifecycleOwner = lifecycleOwner,
        onProgressComplete = { handleEvent(PlaybackEvent.TapNext) }
    )
    private val handler = deps.handler
    private val dataRepository = deps.dataSource
    private val eventLogger = deps.eventLogger
    private val stateMachine = deps.stateMachine
    private val navigator = deps.navigator
    private val repository = deps.repository
    private val playerController = deps.playerController
    private val entryAnimator = deps.entryAnimator
    private val loadEntryUseCase = deps.loadEntryUseCase
    private val preloadAdjacentEntriesUseCase = deps.preloadAdjacentEntriesUseCase
    private val progressManager = deps.progressManager

    private var viewState = StoryViewState()

    /**
     * Opens playback at the provided entry.
     */
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
        val opened = prepareAndShowEntry(resolvedEntryId, animate = false, isPrev = false)
        if (opened) {
            scheduleAdjacentPreload()
        }
        return opened
    }

    /**
     * Handles external and UI playback events.
     */
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
                preloadAdjacentEntriesUseCase.clear()
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
        val loadedEntry = loaded.first
        val targetContainer = loaded.second

        if (currentContainer === targetContainer) {
            onEntryTransitionComplete(loadedEntry.entryId, targetContainer)
            return
        }

        val runAnimation = if (isPrev) entryAnimator::animateToPrevEntry else entryAnimator::animateToNextEntry
        runAnimation(currentContainer, targetContainer) {
            onEntryTransitionComplete(loadedEntry.entryId, targetContainer)
        }
    }

    private fun prepareEntry(entryId: String): Pair<LoadedEntry, StoryCardContainerView>? {
        stateMachine.forceState(PlaybackState.PreparingEntry)
        val loadedEntry = loadEntryUseCase.load(entryId, viewState.entryIds) ?: return null
        val container = repository.setupEntry(
            entryId = loadedEntry.entryId,
            cards = loadedEntry.cards,
            jsonData = jsonData,
            makeDivView = { DivKitSetup.makeView(context, jsonData, lifecycleOwner) }
        )
        return loadedEntry to container
    }

    private fun prepareAndShowEntry(entryId: String, animate: Boolean, isPrev: Boolean): Boolean {
        val prepared = prepareEntry(entryId) ?: return false
        val loadedEntry = prepared.first
        val container = prepared.second
        repository.setCurrentEntry(loadedEntry.entryId)
        viewState = viewState.copy(
            currentEntryId = loadedEntry.entryId,
            currentEntryIndex = loadedEntry.entryIndex,
            currentCards = loadedEntry.cards,
            currentCardIndex = 0
        )
        container.visibility = android.view.View.VISIBLE
        container.showCard(0, animate)
        playerController.prepareVisibleCard(loadedEntry.entryId, 0)
        playerController.activateVisibleCard(loadedEntry.entryId, 0)
        startProgressAndLog(0)
        repository.cleanupToAdjacent(viewState.entryIds, viewState.currentEntryIndex)
        scheduleAdjacentPreload()
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
        val loadedEntry = loadEntryUseCase.load(entryId, viewState.entryIds)
        val cards = loadedEntry?.cards ?: emptyList()
        val entryIndex = loadedEntry?.entryIndex ?: viewState.entryIds.indexOf(entryId).coerceAtLeast(0)
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
        scheduleAdjacentPreload()
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

    private fun scheduleAdjacentPreload() {
        preloadAdjacentEntriesUseCase.schedule(
            entryIds = viewState.entryIds,
            currentEntryIndex = viewState.currentEntryIndex
        )
    }
}

