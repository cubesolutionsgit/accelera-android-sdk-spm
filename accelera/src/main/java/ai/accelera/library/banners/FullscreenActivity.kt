package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.utils.toJsonBytes
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.app.Activity
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject

/**
 * Fullscreen activity for displaying stories with improved architecture.
 */
class FullscreenActivity : Activity() {

    private lateinit var jsonData: ByteArray
    private var viewState = StoryViewState()
    
    private lateinit var rootLayout: FrameLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var progressContainer: ViewGroup
    
    private lateinit var dataRepository: StoryDataRepository
    private lateinit var progressManager: StoryProgressManager
    private lateinit var navigationController: StoryNavigationController
    private lateinit var entryAnimator: StoryEntryAnimator
    private lateinit var gestureHandler: StoryGestureHandler
    
    private var isTransitioning = false
    private var isTransitionComplete = false
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

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
        
        navigationController.updateState(viewState)
        
        // Load initial entry
        loadEntry(entryId)
        
        // Mark initial transition as complete since we're not transitioning between entries
        isTransitionComplete = true
        
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
        }

        // ViewPager for cards
        viewPager = ViewPager2(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isUserInputEnabled = false // Disable swipe - navigation will be via gestures
        }
        
        // Setup page change callback
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Only handle if not transitioning and transition is complete to avoid race conditions
                if (!isTransitioning && isTransitionComplete && !isFinishing && !isDestroyed) {
                    // Verify that position is valid for current cards
                    if (position >= 0 && position < viewState.currentCards.size) {
                        // Verify that adapter matches current state to prevent stale callbacks
                        val currentAdapter = viewPager.adapter as? StoryCardAdapter
                        if (currentAdapter != null && 
                            currentAdapter.entryId == viewState.currentEntryId &&
                            currentAdapter.cards.size == viewState.currentCards.size) {
                            showCard(position)
                        }
                    }
                }
            }
        }
        viewPager.registerOnPageChangeCallback(pageChangeCallback!!)

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

        rootLayout.addView(viewPager)
        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)

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
                navigationController.navigateToNextCard()
            }
        )

        // Initialize navigation controller
        navigationController = StoryNavigationController(
            viewState = viewState,
            onNavigateToCard = { index -> showCard(index) },
            onNavigateToNextEntry = { moveToNextEntry() },
            onNavigateToPrevEntry = { moveToPrevEntry() },
            onFinish = { closeStories() }
        )

        // Initialize entry animator
        entryAnimator = StoryEntryAnimator(rootLayout)

        // Initialize gesture handler
        gestureHandler = StoryGestureHandler(
            context = this,
            rootView = rootLayout,
            listener = object : StoryGestureListener {
                override fun onTapLeft() {
                    navigationController.navigateToPrevCard()
                }

                override fun onTapRight() {
                    navigationController.navigateToNextCard()
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
        
        gestureHandler.setupTouchListener()
    }

    private fun loadEntry(id: String, lastCard: Boolean = false) {
        try {
            // Mark as not transitioning during initial load
            isTransitioning = false
            isTransitionComplete = true
            
            progressManager.stopProgress()

            val cards = dataRepository.loadEntryCards(id) ?: run {
                finish()
                return
            }

            val entryIndex = viewState.entryIds.indexOf(id).coerceAtLeast(0)
            // Always show first card (index 0) when loading entry
            val cardIndex = 0

            viewState = viewState.copy(
                currentEntryId = id,
                currentEntryIndex = entryIndex,
                currentCards = cards,
                currentCardIndex = cardIndex
            )

            navigationController.updateState(viewState)

            // Setup progress bars
            progressManager.setupProgressBars(cards) { card ->
                dataRepository.hasDuration(card)
            }

            // Update adapter
            updateAdapter()

            // Show card
            showCard(cardIndex)
        } catch (e: Exception) {
            Accelera.shared.error("Error in loadEntry: ${e.message}")
            finish()
        }
    }

    private fun updateAdapter() {
        val adapter = StoryCardAdapter(
            jsonData = jsonData,
            entryId = viewState.currentEntryId,
            cards = viewState.currentCards,
            makeDivView = { DivKitSetup.makeView(this, jsonData) }
        )
        
        viewPager.adapter = adapter
    }

    private fun showCard(index: Int) {
        if (isFinishing || isDestroyed) return
        
        // Don't process showCard during entry transitions to avoid race conditions
        // Only allow if transition is complete or we're not transitioning at all
        if (isTransitioning && !isTransitionComplete) {
            return
        }
        
        // Check if cards are available
        if (viewState.currentCards.isEmpty()) {
            Accelera.shared.error("showCard called but currentCards is empty")
            return
        }
        
        if (index < 0) {
            if (!isTransitioning) {
                navigationController.navigateToPrevCard()
            }
            return
        }
        if (index >= viewState.currentCards.size) {
            // If trying to go beyond last card, try to go to next entry
            // But only if we're not already transitioning
            if (!isTransitioning) {
                navigationController.navigateToNextCard()
            }
            return
        }

        try {
            viewState = viewState.copy(currentCardIndex = index)
            navigationController.updateState(viewState)

            // Update ViewPager only if adapter matches
            viewPager.post {
                try {
                    if (isFinishing || isDestroyed) return@post
                    
                    // Double-check we're not transitioning during the post callback
                    if (isTransitioning && !isTransitionComplete) {
                        return@post
                    }
                    
                    val currentAdapter = viewPager.adapter as? StoryCardAdapter
                    if (currentAdapter == null || 
                        currentAdapter.entryId != viewState.currentEntryId ||
                        currentAdapter.cards.size != viewState.currentCards.size) {
                        updateAdapter()
                    }
                    
                    // Only set current item if it's different to avoid unnecessary callbacks
                    if (viewPager.currentItem != index) {
                        viewPager.setCurrentItem(index, false)
                    }
                } catch (e: Exception) {
                    Accelera.shared.error("Error updating ViewPager in showCard: ${e.message}")
                }
            }

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

    private fun moveToNextEntry() {
        // Prevent navigation during transition
        if (isTransitioning) return
        
        // Double-check state before navigation to ensure we have accurate information
        val currentState = viewState
        if (currentState.hasNextEntry && currentState.currentEntryIndex + 1 < currentState.entryIds.size) {
            val nextEntryId = currentState.entryIds[currentState.currentEntryIndex + 1]
            animateToEntry(nextEntryId, false)
        } else {
            closeStories()
        }
    }

    private fun moveToPrevEntry() {
        // Prevent navigation during transition
        if (isTransitioning) return
        
        // Double-check state before navigation to ensure we have accurate information
        val currentState = viewState
        if (currentState.hasPrevEntry && currentState.currentEntryIndex > 0) {
            val prevEntryId = currentState.entryIds[currentState.currentEntryIndex - 1]
            animateToEntry(prevEntryId, true)
        }
    }

    private fun animateToEntry(entryId: String, isPrevEntry: Boolean) {
        if (isTransitioning) return // Prevent multiple simultaneous transitions
        
        try {
            isTransitioning = true
            isTransitionComplete = false // Mark transition as not complete yet
            
            // Stop progress first
            progressManager.stopProgress()

            // Load cards for new entry
            val cards = dataRepository.loadEntryCards(entryId)
            
            if (cards == null || cards.isEmpty()) {
                // If cards failed to load or empty, don't close - just stop transition
                Accelera.shared.error("Failed to load cards for entry: $entryId")
                isTransitioning = false
                isTransitionComplete = true
                return
            }
            
            val entryIndex = viewState.entryIds.indexOf(entryId).coerceAtLeast(0)
            // Always show first card (index 0) when transitioning to a new group
            val cardIndex = 0

            // Update state AFTER successful card loading
            viewState = viewState.copy(
                currentEntryId = entryId,
                currentEntryIndex = entryIndex,
                currentCards = cards,
                currentCardIndex = cardIndex
            )

            navigationController.updateState(viewState)

            // Setup progress bars
            progressManager.setupProgressBars(cards) { card ->
                dataRepository.hasDuration(card)
            }

            // Update adapter first
            updateAdapter()
            
            // Wait for adapter to be ready before setting item
            viewPager.post {
                try {
                    // Check if view is still valid
                    if (!isFinishing && !isDestroyed) {
                        // Set initial card position (first card, index 0)
                        // This might trigger onPageSelected, but isTransitioning is still true
                        viewPager.setCurrentItem(cardIndex, false)
                        
                        // Animate ViewPager transition
                        val screenWidth = rootLayout.width.toFloat()
                        if (screenWidth > 0) {
                            val startTranslationX = if (isPrevEntry) -screenWidth else screenWidth
                            
                            // Cancel any ongoing animation
                            viewPager.animate().cancel()
                            
                            // Set initial position
                            viewPager.translationX = startTranslationX
                            viewPager.alpha = 0.5f

                            // Animate in
                            viewPager.animate()
                                .translationX(0f)
                                .alpha(1f)
                                .setDuration(300L)
                                .setListener(object : android.animation.AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: android.animation.Animator) {
                                        try {
                                            if (!isFinishing && !isDestroyed) {
                                                viewPager.translationX = 0f
                                                viewPager.alpha = 1f
                                                
                                                // Start progress for the card before marking transition complete
                                                // Don't call showCard here to avoid navigation logic
                                                // Just start the progress animation directly
                                                viewPager.post {
                                                    if (!isFinishing && !isDestroyed) {
                                                        val card = viewState.currentCards.getOrNull(cardIndex)
                                                        if (card != null && cardIndex < viewState.currentCards.size) {
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
                                                            
                                                            // Start progress animation
                                                            val duration = dataRepository.getCardDuration(card)
                                                            progressManager.showCard(cardIndex, duration)
                                                            
                                                            // Mark transition as complete AFTER progress starts
                                                            // This ensures onPageSelected can safely handle callbacks
                                                            isTransitionComplete = true
                                                            isTransitioning = false
                                                        } else {
                                                            // Fallback if card is null
                                                            isTransitionComplete = true
                                                            isTransitioning = false
                                                        }
                                                    } else {
                                                        isTransitionComplete = true
                                                        isTransitioning = false
                                                    }
                                                }
                                            } else {
                                                isTransitionComplete = true
                                                isTransitioning = false
                                            }
                                        } catch (e: Exception) {
                                            Accelera.shared.error("Error in animation end: ${e.message}")
                                            isTransitionComplete = true
                                            isTransitioning = false
                                        }
                                    }
                                    
                                    override fun onAnimationCancel(animation: android.animation.Animator) {
                                        isTransitionComplete = true
                                        isTransitioning = false
                                    }
                                })
                                .start()
                        } else {
                            // View not measured yet, skip animation
                            // Still need to start progress and mark transition complete
                            viewPager.post {
                                if (!isFinishing && !isDestroyed) {
                                    val card = viewState.currentCards.getOrNull(cardIndex)
                                    if (card != null) {
                                        val duration = dataRepository.getCardDuration(card)
                                        progressManager.showCard(cardIndex, duration)
                                    }
                                }
                                isTransitionComplete = true
                                isTransitioning = false
                            }
                        }
                    } else {
                        isTransitionComplete = true
                        isTransitioning = false
                    }
                } catch (e: Exception) {
                    Accelera.shared.error("Error in animateToEntry post: ${e.message}")
                    isTransitionComplete = true
                    isTransitioning = false
                }
            }
        } catch (e: Exception) {
            Accelera.shared.error("Error in animateToEntry: ${e.message}")
            isTransitionComplete = true
            isTransitioning = false
        }
    }

    private fun closeStories() {
        entryAnimator.animateClose(rootLayout) {
            finish()
            // Use default transition instead of 0,0 to avoid resource ID errors
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause progress when activity is paused (app minimized)
        progressManager.pauseProgress()
    }

    override fun onResume() {
        super.onResume()
        // Resume progress when activity is resumed (app restored)
        progressManager.resumeProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pageChangeCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
            viewPager.animate().cancel()
            progressManager.cleanup()
            entryAnimator.reset()
        } catch (e: Exception) {
            Accelera.shared.error("Error in onDestroy: ${e.message}")
        }
    }
}
