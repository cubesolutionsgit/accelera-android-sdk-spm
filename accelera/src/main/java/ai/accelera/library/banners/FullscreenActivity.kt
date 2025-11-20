package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.utils.toJsonBytes
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.json.JSONObject

/**
 * Fullscreen activity for displaying stories with improved architecture.
 * Uses custom View instead of ViewPager2 for better control and preloading.
 */
class FullscreenActivity : Activity() {

    private lateinit var jsonData: ByteArray
    private var viewState = StoryViewState()
    
    private lateinit var rootLayout: FrameLayout
    private lateinit var currentCardContainer: StoryCardContainerView
    private lateinit var progressContainer: ViewGroup
    
    private lateinit var dataRepository: StoryDataRepository
    private lateinit var progressManager: StoryProgressManager
    private lateinit var navigationUseCase: StoryNavigationUseCase
    private lateinit var entryPreloader: StoryEntryPreloader
    private lateinit var entryAnimator: StoryEntryAnimator
    private lateinit var gestureHandler: StoryGestureHandler
    
    private var isTransitioning = false
    private var transitionStartTime = 0L
    private val minTransitionDuration = 200L  // Minimum transition duration for smooth UX
    private val entryContainers = mutableMapOf<String, StoryCardContainerView>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsonData = intent.getByteArrayExtra("jsonData") ?: return finish()
        val entryId = intent.getStringExtra("entryId") ?: return finish()

        setupUI()
        initializeComponents()
        
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

    private fun setupUI() {
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

        // Current card container
        currentCardContainer = StoryCardContainerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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
        rootLayout.addView(currentCardContainer)
        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)
        
        // Ensure progress bars are visible above cards
        progressContainer.elevation = 10f
        closeButton.elevation = 20f

        setContentView(rootLayout)
    }

    private fun initializeComponents() {
        // Initialize data repository
        dataRepository = StoryDataRepository(jsonData)

        // Initialize progress manager
        progressManager = StoryProgressManager(
            context = this,
            progressContainer = progressContainer,
            onProgressComplete = {
                handleNavigation(navigationUseCase.navigateToNext(viewState))
            }
        )

        // Initialize navigation use case
        navigationUseCase = StoryNavigationUseCase()

        // Initialize entry preloader
        entryPreloader = StoryEntryPreloader(
            context = this,
            jsonData = jsonData,
            dataRepository = dataRepository,
            makeDivView = { DivKitSetup.makeView(this, jsonData) }
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
        gestureHandler.isTransitioning = { isTransitioning }
        
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
        try {
            if (!isInitialLoad) {
                isTransitioning = true
            }
            
            progressManager.stopProgress()

            // Try to get cached cards first
            val cards = entryPreloader.getCachedCards(id) 
                ?: dataRepository.loadEntryCards(id) 
                ?: run {
                    finish()
                    return
                }

            val entryIndex = viewState.entryIds.indexOf(id).coerceAtLeast(0)
            val cardIndex = 0

            viewState = viewState.copy(
                currentEntryId = id,
                currentEntryIndex = entryIndex,
                currentCards = cards,
                currentCardIndex = cardIndex
            )

            // Preload adjacent entries
            entryPreloader.preloadAdjacentEntries(viewState.entryIds, entryIndex)

            // Setup progress bars
            progressManager.setupProgressBars(cards) { card ->
                dataRepository.hasDuration(card)
            }

            // Setup card container with cached or new views
            setupCardContainer(id, cards, isInitialLoad)

            // Show first card
            showCard(cardIndex, animate = !isInitialLoad)
            
            if (!isInitialLoad) {
                isTransitioning = false
            }
        } catch (e: Exception) {
            Accelera.shared.error("Error in loadEntry: ${e.message}")
            finish()
        }
    }

    /**
     * Sets up the card container with cards.
     * Uses cached views from preloader if available.
     */
    private fun setupCardContainer(entryId: String, cards: List<JSONObject>, isInitialLoad: Boolean) {
        // Get or create container for this entry
        val container = entryContainers.getOrPut(entryId) {
            if (entryId == viewState.currentEntryId) {
                // Use current container
                currentCardContainer
            } else {
                // Create new container for preloaded entry
                StoryCardContainerView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    visibility = View.GONE
                    // Ensure it doesn't intercept touch events
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    rootLayout.addView(this)
                }
            }
        }

        // Setup cards using cached views if available
        val cachedViews = entryPreloader.getCachedCardViews(entryId)
        if (cachedViews != null && cachedViews.size == cards.size) {
            // Use cached views
            container.setupCardsWithViews(
                cards = cards,
                entryId = entryId,
                jsonData = jsonData,
                cachedViews = cachedViews
            )
        } else {
            // Create new views
            container.setupCards(
                cards = cards,
                entryId = entryId,
                jsonData = jsonData,
                makeDivView = { DivKitSetup.makeView(this, jsonData) }
            )
        }

        // Update current container reference
        if (entryId == viewState.currentEntryId) {
            currentCardContainer = container
        }
    }

    /**
     * Shows a specific card.
     */
    private fun showCard(index: Int, animate: Boolean = true) {
        if (isFinishing || isDestroyed) return
        
        // Don't process during transitions
        if (isTransitioning) {
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

        try {
            viewState = viewState.copy(currentCardIndex = index)

            // Show card in container
            currentCardContainer.showCard(index, animate = animate)

            // Log view event
            try {
                val card = viewState.currentCards[index]
                val meta = dataRepository.getCardMeta(card)
                val eventPayload = mapOf(
                    "event" to "view",
                    "meta" to meta.toString()
                )
                Accelera.shared.logEvent(eventPayload.toJsonBytes())
            } catch (e: Exception) {
                Accelera.shared.error("Error logging view event: ${e.message}")
            }

            // Start progress animation
            val card = viewState.currentCards[index]
            val duration = dataRepository.getCardDuration(card)
            progressManager.showCard(index, duration)
        } catch (e: Exception) {
            Accelera.shared.error("Error in showCard: ${e.message}")
        }
    }

    /**
     * Moves to the next entry with animation.
     * Used for swipes - always switches to next entry group regardless of current card position.
     * Includes protection against rapid consecutive swipes.
     */
    private fun moveToNextEntry() {
        // Enhanced protection: check both isTransitioning and time since last transition
        val currentTime = System.currentTimeMillis()
        if (isTransitioning || (currentTime - transitionStartTime < minTransitionDuration)) {
            return
        }
        
        // Always try to move to next entry (swipe behavior)
        if (viewState.hasNextEntry && viewState.currentEntryIndex + 1 < viewState.entryIds.size) {
            val nextEntryId = viewState.entryIds[viewState.currentEntryIndex + 1]
            animateToEntry(nextEntryId, isPrevEntry = false)
        } else {
            // Last entry - close stories (with delay to prevent accidental close)
            handler.postDelayed({
                if (!isTransitioning) {
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
        // Enhanced protection: check both isTransitioning and time since last transition
        val currentTime = System.currentTimeMillis()
        if (isTransitioning || (currentTime - transitionStartTime < minTransitionDuration)) {
            return
        }
        
        // Always try to move to previous entry (swipe behavior)
        if (viewState.hasPrevEntry && viewState.currentEntryIndex > 0) {
            val prevEntryId = viewState.entryIds[viewState.currentEntryIndex - 1]
            animateToEntry(prevEntryId, isPrevEntry = true)
        } else {
            // First entry - close stories (with delay to prevent accidental close)
            handler.postDelayed({
                if (!isTransitioning) {
                    closeStories()
                }
            }, 100)
        }
    }

    /**
     * Animates transition to a new entry.
     */
    private fun animateToEntry(entryId: String, isPrevEntry: Boolean) {
        // Enhanced protection: check both flag and time
        val currentTime = System.currentTimeMillis()
        if (isTransitioning || (currentTime - transitionStartTime < minTransitionDuration)) {
            return
        }
        
        try {
            isTransitioning = true
            transitionStartTime = currentTime
            progressManager.stopProgress()

            // Ensure entry is preloaded
            if (!entryPreloader.isCached(entryId)) {
                entryPreloader.preloadAdjacentEntries(viewState.entryIds, viewState.currentEntryIndex)
            }

            // Get or create container for target entry
            val targetContainer = entryContainers.getOrPut(entryId) {
                StoryCardContainerView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    visibility = View.GONE
                    // Ensure it doesn't intercept touch events
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    rootLayout.addView(this)
                }
            }

            // Load cards if container is not set up
            val cards = entryPreloader.getCachedCards(entryId) 
                ?: dataRepository.loadEntryCards(entryId)
                ?: run {
                    Accelera.shared.error("Failed to load cards for entry: $entryId")
                    isTransitioning = false
                    return
                }

            // Setup container if needed
            if (targetContainer.getCardCount() == 0) {
                setupCardContainer(entryId, cards, isInitialLoad = false)
            }

            val entryIndex = viewState.entryIds.indexOf(entryId).coerceAtLeast(0)
            val cardIndex = 0

            // Update state
            viewState = viewState.copy(
                currentEntryId = entryId,
                currentEntryIndex = entryIndex,
                currentCards = cards,
                currentCardIndex = cardIndex
            )

            // Preload adjacent entries
            entryPreloader.preloadAdjacentEntries(viewState.entryIds, entryIndex)

            // Setup progress bars
            progressManager.setupProgressBars(cards) { card ->
                dataRepository.hasDuration(card)
            }

            // Show first card in target container
            targetContainer.showCard(cardIndex, animate = false)

            // Animate transition
            if (isPrevEntry) {
                entryAnimator.animateToPrevEntry(currentCardContainer, targetContainer) {
                    // Cleanup old container if not needed
                    cleanupOldContainers()
                    currentCardContainer = targetContainer
                    
                    // Reset gesture handler state after transition completes
                    gestureHandler.resetState()
                    
                    // Start progress
                    val card = cards[cardIndex]
                    val duration = dataRepository.getCardDuration(card)
                    progressManager.showCard(cardIndex, duration)
                    
                    // Log view event
                    try {
                        val meta = dataRepository.getCardMeta(card)
                        val eventPayload = mapOf(
                            "event" to "view",
                            "meta" to meta.toString()
                        )
                        Accelera.shared.logEvent(eventPayload.toJsonBytes())
                    } catch (e: Exception) {
                        Accelera.shared.error("Error logging view event: ${e.message}")
                    }
                    
                    // Mark transition complete after a small delay to ensure smooth UX
                    handler.postDelayed({
                        isTransitioning = false
                    }, 50)
                }
            } else {
                entryAnimator.animateToNextEntry(currentCardContainer, targetContainer) {
                    // Cleanup old container if not needed
                    cleanupOldContainers()
                    currentCardContainer = targetContainer
                    
                    // Reset gesture handler state after transition completes
                    gestureHandler.resetState()
                    
                    // Start progress
                    val card = cards[cardIndex]
                    val duration = dataRepository.getCardDuration(card)
                    progressManager.showCard(cardIndex, duration)
                    
                    // Log view event
                    try {
                        val meta = dataRepository.getCardMeta(card)
                        val eventPayload = mapOf(
                            "event" to "view",
                            "meta" to meta.toString()
                        )
                        Accelera.shared.logEvent(eventPayload.toJsonBytes())
                    } catch (e: Exception) {
                        Accelera.shared.error("Error logging view event: ${e.message}")
                    }
                    
                    // Mark transition complete after a small delay to ensure smooth UX
                    handler.postDelayed({
                        isTransitioning = false
                    }, 50)
                }
            }
        } catch (e: Exception) {
            Accelera.shared.error("Error in animateToEntry: ${e.message}")
            isTransitioning = false
        }
    }

    /**
     * Cleans up containers that are no longer needed.
     */
    private fun cleanupOldContainers() {
        val currentEntryId = viewState.currentEntryId
        val currentIndex = viewState.currentEntryIndex
        
        // Keep only current and adjacent entries
        val entriesToKeep = mutableSetOf<String>()
        if (currentIndex >= 0 && currentIndex < viewState.entryIds.size) {
            entriesToKeep.add(viewState.entryIds[currentIndex])
        }
        if (currentIndex > 0) {
            entriesToKeep.add(viewState.entryIds[currentIndex - 1])
        }
        if (currentIndex >= 0 && currentIndex + 1 < viewState.entryIds.size) {
            entriesToKeep.add(viewState.entryIds[currentIndex + 1])
        }
        
        // Remove containers that are not needed
        entryContainers.keys.toList().forEach { entryId ->
            if (!entriesToKeep.contains(entryId) && entryId != currentEntryId) {
                val container = entryContainers.remove(entryId)
                container?.let {
                    if (it != currentCardContainer) {
                        rootLayout.removeView(it)
                        it.cleanup()
                    }
                }
            }
        }
    }

    private fun closeStories() {
        // Protection: don't close if transitioning (prevents accidental closes during rapid swipes)
        if (isTransitioning) {
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
            entryContainers.values.forEach { container ->
                rootLayout.removeView(container)
                container.cleanup()
            }
            entryContainers.clear()
            
            // Cleanup preloader
            entryPreloader.clearCache()
            
            // Cleanup other components
            progressManager.cleanup()
            entryAnimator.reset()
        } catch (e: Exception) {
            Accelera.shared.error("Error in onDestroy: ${e.message}")
        }
    }
}
