package ai.accelera.library.banners.presentation.ui

import ai.accelera.library.Accelera
import ai.accelera.library.banners.presentation.ui.StoryCardContainerView
import ai.accelera.library.banners.data.repository.StoryDataRepository
import ai.accelera.library.banners.domain.model.StoryViewState
import ai.accelera.library.banners.domain.usecase.StoryNavigationUseCase
import ai.accelera.library.banners.infrastructure.cache.StoryEntryPreloader
import ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureHandler
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureListener
import ai.accelera.library.banners.presentation.manager.StoryTransitionManager
import ai.accelera.library.banners.presentation.manager.StoryContainerManager
import ai.accelera.library.banners.infrastructure.logging.StoryEventLogger
import ai.accelera.library.banners.presentation.usecase.StoryCardDisplayUseCase
import ai.accelera.library.banners.presentation.usecase.StoryEntryLoader
import ai.accelera.library.banners.presentation.manager.StoryProgressManager
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Fullscreen activity for displaying stories with improved architecture.
 * Uses custom View instead of ViewPager2 for better control and preloading.
 * 
 * Refactored to follow SOLID principles and DRY:
 * - Delegates responsibilities to specialized classes
 * - No code duplication
 * - Easy to test and extend
 */
class FullscreenActivity : AppCompatActivity() {

    private lateinit var jsonData: ByteArray
    private var viewState = StoryViewState()

    private lateinit var rootLayout: FrameLayout
    private lateinit var progressContainer: ViewGroup

    // Core components
    private lateinit var dataRepository: StoryDataRepository
    private lateinit var progressManager: StoryProgressManager
    private lateinit var navigationUseCase: StoryNavigationUseCase
    private lateinit var entryPreloader: StoryEntryPreloader
    private lateinit var entryAnimator: StoryEntryAnimator
    private lateinit var gestureHandler: StoryGestureHandler

    // Refactored components
    private lateinit var transitionManager: StoryTransitionManager
    private lateinit var containerManager: StoryContainerManager
    private lateinit var eventLogger: StoryEventLogger
    private lateinit var cardDisplayUseCase: StoryCardDisplayUseCase
    private lateinit var entryLoader: StoryEntryLoader

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsonData = intent.getByteArrayExtra("jsonData") ?: return finish()
        val entryId = intent.getStringExtra("entryId") ?: return finish()

        val initialContainer = setupUI()
        initializeComponents(initialContainer)

        // Load entry IDs
        val entryIds = dataRepository.loadEntryIds()
        val entryIndex = entryIds.indexOf(entryId).coerceAtLeast(0)

        viewState = viewState.copy(
            currentEntryId = entryId,
            currentEntryIndex = entryIndex,
            entryIds = entryIds
        )

        // Preload adjacent entries
        entryPreloader.preloadAdjacentEntries(entryIds, entryIndex)

        // Load initial entry
        loadEntry(entryId, isInitialLoad = true)

        // Animate opening (from bottom to top)
        rootLayout.post {
            entryAnimator.animateOpen(rootLayout) {}
        }
    }

    private fun setupUI(): StoryCardContainerView {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Progress bars container
        progressContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginStart = (8 * resources.displayMetrics.density).toInt()
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            // Don't intercept touch events - let gesture handler process them
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }

        // Initial card container
        val initialContainer = StoryCardContainerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Don't intercept touch events - let gesture handler process them
            isClickable = false
            isFocusable = false
        }

        // Close button
        val closeButton = CloseButton(this).apply {
            setOnClickListener { closeStories() }
            layoutParams = FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
                marginEnd = (16 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
            }
        }

        // Add views in correct z-order: card container first, then progress bars on top, then close button
        rootLayout.addView(initialContainer)
        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)

        // Ensure progress bars are visible above cards
        progressContainer.elevation = 10f
        closeButton.elevation = 20f

        setContentView(rootLayout)
        
        return initialContainer
    }

    private fun initializeComponents(initialContainer: StoryCardContainerView) {
        // Initialize data repository
        dataRepository = StoryDataRepository(jsonData)

        // Initialize transition manager
        transitionManager = StoryTransitionManager()

        // Initialize container manager
        containerManager = StoryContainerManager(this, rootLayout)
        containerManager.setCurrentContainer(initialContainer)

        // Initialize event logger
        eventLogger = StoryEventLogger(dataRepository)

        // Initialize progress manager
        progressManager = StoryProgressManager(
            context = this,
            progressContainer = progressContainer,
            onProgressComplete = {
                handleNavigation(navigationUseCase.navigateToNext(viewState))
            }
        )

        // Initialize card display use case
        cardDisplayUseCase = StoryCardDisplayUseCase(
            progressManager = progressManager,
            eventLogger = eventLogger,
            dataRepository = dataRepository
        )

        // Initialize navigation use case
        navigationUseCase = StoryNavigationUseCase()

        // Initialize entry preloader
        entryPreloader = StoryEntryPreloader(
            context = this,
            jsonData = jsonData,
            dataRepository = dataRepository,
            makeDivView = { DivKitSetup.makeView(this, jsonData, this) }
        )

        // Initialize entry loader
        entryLoader = StoryEntryLoader(
            dataRepository = dataRepository,
            entryPreloader = entryPreloader,
            containerManager = containerManager,
            progressManager = progressManager,
            jsonData = jsonData,
            context = this,
            lifecycleOwner = this
        )

        // Initialize entry animator
        entryAnimator = StoryEntryAnimator(rootLayout)

        // Initialize gesture handler
        gestureHandler = StoryGestureHandler(
            context = this,
            rootView = rootLayout,
            listener = object : StoryGestureListener {
                override fun onTapLeft() {
                    handleNavigation(navigationUseCase.navigateToPrev(viewState))
                }

                override fun onTapRight() {
                    handleNavigation(navigationUseCase.navigateToNext(viewState))
                }

                override fun onSwipeLeft() {
                    moveToNextEntry()
                }

                override fun onSwipeRight() {
                    moveToPrevEntry()
                }

                override fun onSwipeDown() {
                    closeStories()
                }

                override fun onLongPress() {
                    progressManager.pauseProgress()
                }

                override fun onLongPressEnd() {
                    progressManager.resumeProgress()
                }
            }
        )

        // Set transition check callback
        gestureHandler.isTransitioning = { transitionManager.isTransitioning() }

        gestureHandler.setupTouchListener()
    }

    /**
     * Handles navigation result from use case.
     */
    private fun handleNavigation(result: StoryNavigationUseCase.NavigationResult) {
        when (result) {
            is StoryNavigationUseCase.NavigationResult.NavigateToCard -> {
                showCard(result.index)
            }

            is StoryNavigationUseCase.NavigationResult.NavigateToNextEntry -> {
                moveToNextEntry()
            }

            is StoryNavigationUseCase.NavigationResult.NavigateToPrevEntry -> {
                moveToPrevEntry()
            }

            is StoryNavigationUseCase.NavigationResult.Finish -> {
                closeStories()
            }

            is StoryNavigationUseCase.NavigationResult.NoAction -> {
                // Do nothing
            }
        }
    }

    /**
     * Loads an entry and sets up the card container.
     */
    private fun loadEntry(id: String, isInitialLoad: Boolean = false) {
        if (!isInitialLoad && !transitionManager.canStartTransition()) {
            return
        }

        if (!isInitialLoad) {
            transitionManager.startTransition()
        }

        progressManager.stopProgress()

        when (val result = entryLoader.loadEntry(id, viewState, isInitialLoad)) {
            is StoryEntryLoader.LoadResult.Success -> {
                viewState = result.viewState

                // Get container for this entry (should be set up by entryLoader)
                val container = containerManager.getCurrentContainer() 
                    ?: containerManager.getOrCreateContainer(id, viewState.currentEntryId)
                
                // Ensure container is visible
                container.visibility = android.view.View.VISIBLE
                
                val displayResult = cardDisplayUseCase.displayCard(
                    viewState = viewState,
                    cardIndex = 0,
                    container = container,
                    animate = !isInitialLoad,
                    isFinishing = isFinishing,
                    isDestroyed = isDestroyed,
                    isTransitioning = transitionManager.isTransitioning()
                )

                if (displayResult == StoryCardDisplayUseCase.DisplayResult.Success && !isInitialLoad) {
                    transitionManager.completeTransition()
                }
            }
            is StoryEntryLoader.LoadResult.Failed -> {
                finish()
            }
        }
    }

    /**
     * Shows a specific card.
     */
    private fun showCard(index: Int, animate: Boolean = true) {
        if (isFinishing || isDestroyed) return

        // Don't process during transitions
        if (transitionManager.isTransitioning()) {
            return
        }

        // Check if cards are available
        if (viewState.currentCards.isEmpty()) {
            Accelera.shared.error("showCard called but currentCards is empty")
            return
        }

        // Validate index
        if (index < 0 || index >= viewState.currentCards.size) {
            // Try to navigate to adjacent entry
            val result = navigationUseCase.navigateToCard(viewState, index)
            handleNavigation(result)
            return
        }

        viewState = viewState.copy(currentCardIndex = index)

        val container = containerManager.getCurrentContainer() ?: return
        cardDisplayUseCase.displayCard(
            viewState = viewState,
            cardIndex = index,
            container = container,
            animate = animate,
            isFinishing = isFinishing,
            isDestroyed = isDestroyed,
            isTransitioning = transitionManager.isTransitioning()
        )
    }

    /**
     * Moves to the next entry with animation.
     * Used for swipes - always switches to next entry group regardless of current card position.
     * Includes protection against rapid consecutive swipes.
     */
    private fun moveToNextEntry() {
        if (!transitionManager.canStartTransition()) {
            return
        }

        // Always try to move to next entry (swipe behavior)
        if (viewState.hasNextEntry && viewState.currentEntryIndex + 1 < viewState.entryIds.size) {
            val nextEntryId = viewState.entryIds[viewState.currentEntryIndex + 1]
            animateToEntry(nextEntryId, isPrevEntry = false)
        } else {
            // Last entry - close stories (with delay to prevent accidental close)
            handler.postDelayed({
                if (!transitionManager.isTransitioning()) {
                    closeStories()
                }
            }, 100)
        }
    }

    /**
     * Moves to the previous entry with animation.
     * Used for swipes - always switches to previous entry group regardless of current card position.
     * If this is the first entry, closes stories (similar to last entry behavior).
     * Includes protection against rapid consecutive swipes.
     */
    private fun moveToPrevEntry() {
        if (!transitionManager.canStartTransition()) {
            return
        }

        // Always try to move to previous entry (swipe behavior)
        if (viewState.hasPrevEntry && viewState.currentEntryIndex > 0) {
            val prevEntryId = viewState.entryIds[viewState.currentEntryIndex - 1]
            animateToEntry(prevEntryId, isPrevEntry = true)
        } else {
            // First entry - close stories (with delay to prevent accidental close)
            handler.postDelayed({
                if (!transitionManager.isTransitioning()) {
                    closeStories()
                }
            }, 100)
        }
    }

    /**
     * Animates transition to a new entry.
     * Refactored to use new components and eliminate code duplication.
     */
    private fun animateToEntry(entryId: String, isPrevEntry: Boolean) {
        if (!transitionManager.canStartTransition()) {
            return
        }

        try {
            transitionManager.startTransition()
            progressManager.stopProgress()

            // Ensure entry is preloaded
            if (!entryPreloader.isCached(entryId)) {
                entryPreloader.preloadAdjacentEntries(
                    viewState.entryIds,
                    viewState.currentEntryIndex
                )
            }

            // Load entry using entry loader
            when (val result = entryLoader.loadEntry(entryId, viewState, isInitialLoad = false)) {
                is StoryEntryLoader.LoadResult.Success -> {
                    viewState = result.viewState

                    // Get target container
                    val currentContainer = containerManager.getCurrentContainer() ?: return
                    val targetContainer = containerManager.getOrCreateContainer(
                        entryId,
                        viewState.currentEntryId
                    )

                    // Ensure target container is visible
                    targetContainer.visibility = android.view.View.VISIBLE
                    
                    // Show first card in target container
                    targetContainer.showCard(0, animate = false)

                    // Animate transition
                    val animateTransition = if (isPrevEntry) {
                        entryAnimator::animateToPrevEntry
                    } else {
                        entryAnimator::animateToNextEntry
                    }

                    animateTransition(currentContainer, targetContainer) {
                        onTransitionComplete(targetContainer)
                    }
                }
                is StoryEntryLoader.LoadResult.Failed -> {
                    Accelera.shared.error("Failed to load cards for entry: $entryId")
                    transitionManager.completeTransition()
                }
            }
        } catch (e: Exception) {
            Accelera.shared.error("Error in animateToEntry: ${e.message}")
            transitionManager.completeTransition()
        }
    }

    /**
     * Handles transition completion.
     * Extracted common logic to eliminate duplication (DRY).
     */
    private fun onTransitionComplete(targetContainer: StoryCardContainerView) {
        // Cleanup old containers
        containerManager.cleanupOldContainers(
            viewState.currentEntryId,
            viewState.currentEntryIndex,
            viewState.entryIds
        )

        // Update current container
        containerManager.setCurrentContainer(targetContainer)

        // Reset gesture handler state after transition completes
        gestureHandler.resetState()

        // Display first card with progress and event logging
        val container = containerManager.getCurrentContainer() ?: return
        cardDisplayUseCase.displayCard(
            viewState = viewState,
            cardIndex = 0,
            container = container,
            animate = false,
            isFinishing = isFinishing,
            isDestroyed = isDestroyed,
            isTransitioning = false
        )

        // Mark transition complete after a small delay to ensure smooth UX
        handler.postDelayed({
            transitionManager.completeTransition()
        }, 50)
    }

    private fun closeStories() {
        // Protection: don't close if transitioning (prevents accidental closes during rapid swipes)
        if (transitionManager.isTransitioning()) {
            return
        }

        entryAnimator.animateClose(rootLayout) {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onPause() {
        super.onPause()
        progressManager.pauseProgress()
    }

    override fun onResume() {
        super.onResume()
        progressManager.resumeProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Cleanup all containers
            containerManager.cleanup()

            // Cleanup preloader
            entryPreloader.clearCache()

            // Cleanup other components
            progressManager.cleanup()
            entryAnimator.reset()
            transitionManager.reset()
        } catch (e: Exception) {
            Accelera.shared.error("Error in onDestroy: ${e.message}")
        }
    }
}

