package ai.accelera.library.banners.infrastructure.gesture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

/**
 * Interface for gesture callbacks.
 */
interface StoryGestureListener {
    fun onTapLeft()
    fun onTapRight()
    fun onSwipeLeft()
    fun onSwipeRight()
    fun onSwipeDown()
    fun onLongPress()
    fun onLongPressEnd()
}

/**
 * Handles all gesture interactions for stories.
 * Supports taps and swipes with protection against accidental actions.
 */
class StoryGestureHandler(
    context: Context,
    private val rootView: ViewGroup,
    private val listener: StoryGestureListener
) {
    // Callback to check if transitions are in progress
    var isTransitioning: () -> Boolean = { false }
    
    // Reduced thresholds for better sensitivity, especially for short swipes
    private val swipeThreshold = 50 * context.resources.displayMetrics.density
    private val minSwipeVelocity = 300 * context.resources.displayMetrics.density
    private val minDragDistance = 5 * context.resources.displayMetrics.density
    private val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
    private val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
    
    // For short swipes, use percentage of screen instead of fixed pixels
    private val shortSwipeThreshold = screenWidth * 0.15f  // 15% of screen width
    private val shortSwipeVelocity = 200 * context.resources.displayMetrics.density
    
    // For closing stories - require explicit vertical gesture but allow shorter swipes
    private val closeSwipeThreshold = screenHeight * 0.15f  // 15% of screen height for closing (reduced from 25%)
    private val closeSwipeVelocity = 300 * context.resources.displayMetrics.density  // Reduced from 400

    private var initialX = 0f
    private var initialY = 0f
    private var isSwipeDetected = false
    private var isGestureHandled = false
    private var isDragging = false
    private var isLongPressing = false
    private var lastMoveX = 0f
    private var lastMoveY = 0f
    
    // Protection against rapid gestures
    private var lastGestureTime = 0L
    private val minGestureInterval = 150L  // Minimum 150ms between gestures
    private val handler = Handler(Looper.getMainLooper())
    private var pendingSwipeAction: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isLongPressing = true
                listener.onLongPress()
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Don't handle tap if we were dragging (swipe detected)
                // This ensures swipes work everywhere, including middle area
                if (isDragging || isSwipeDetected) {
                    return false
                }

                val screenWidth = rootView.width.toFloat()
                val tapX = e.x

                // Check if tap is on close button area (top-right corner)
                val closeButtonSize = (24 * context.resources.displayMetrics.density)
                val closeButtonMargin = (16 * context.resources.displayMetrics.density)
                if (tapX > screenWidth - closeButtonSize - closeButtonMargin * 2 &&
                    e.y < closeButtonSize + closeButtonMargin * 2
                ) {
                    // Tap is in close button area, let it handle
                    return false
                }

                // Instagram-like behavior: taps for navigation work only on edges (left and right thirds)
                // Middle third taps are ignored for navigation, but don't block other interactions
                // Swipes work everywhere regardless of X position
                val leftThird = screenWidth / 3f
                val rightThird = screenWidth * 2f / 3f

                when {
                    tapX < leftThird -> {
                        // Tap on left edge - navigate to previous
                        listener.onTapLeft()
                        return true
                    }
                    tapX > rightThird -> {
                        // Tap on right edge - navigate to next
                        listener.onTapRight()
                        return true
                    }
                    else -> {
                        // Tap in middle third - ignore for navigation (return false to allow other handlers)
                        // This doesn't block swipes or other interactions
                        return false
                    }
                }
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false

                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                val absDeltaX = abs(deltaX)
                val absDeltaY = abs(deltaY)
                val absVelocityX = abs(velocityX)
                val absVelocityY = abs(velocityY)

                // Determine if horizontal or vertical swipe
                // For horizontal: X movement should be significantly more than Y
                // For vertical (closing): Y movement should be significantly more than X AND X should be minimal
                val isHorizontal = absDeltaX > absDeltaY * 1.5f  // 50% more horizontal than vertical (stricter)
                val isVertical = absDeltaY > absDeltaX * 2.0f && absDeltaX < screenWidth * 0.1f  // Y must be 2x X AND X must be < 10% of screen
                
                if (isHorizontal) {
                    // Horizontal swipe - navigate between entries
                    // More lenient: accept if distance OR velocity is sufficient
                    val distanceOk = absDeltaX > shortSwipeThreshold
                    val velocityOk = absVelocityX > shortSwipeVelocity
                    
                    // Accept if either condition is met (more sensitive)
                    if (distanceOk || velocityOk) {
                        // Check if enough time has passed since last gesture
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastGestureTime < minGestureInterval) {
                            // Too soon, ignore this gesture
                            return false
                        }
                        
                        if (deltaX > 0) {
                            // Swipe right - previous entry
                            executeSwipeAction { listener.onSwipeRight() }
                        } else {
                            // Swipe left - next entry
                            executeSwipeAction { listener.onSwipeLeft() }
                        }
                        isGestureHandled = true
                        return true
                    }
                } else if (isVertical) {
                    // Vertical swipe - require more explicit gesture for closing
                    // Only if Y is significantly more than X AND X movement is minimal
                    val distanceOk = absDeltaY > closeSwipeThreshold
                    val velocityOk = absVelocityY > closeSwipeVelocity
                    
                    // Require BOTH distance AND velocity for closing (more protection)
                    if (distanceOk && velocityOk && deltaY > 0 && velocityY > 0) {
                        // Swipe down - close stories (only if explicit enough)
                        listener.onSwipeDown()
                        isGestureHandled = true
                        return true
                    }
                }
                // If neither horizontal nor vertical is clear, ignore (prevents accidental closes)

                return false
            }
        }
    )

    /**
     * Sets up touch listener on the root view.
     * Ensures all touch events are properly handled.
     */
    fun setupTouchListener() {
        // Make sure root view can receive touch events
        rootView.isClickable = true
        rootView.isFocusable = true
        
        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Don't start new gesture if transitioning
                    if (isTransitioning()) {
                        return@setOnTouchListener false
                    }
                    
                    initialX = event.x
                    initialY = event.y
                    isSwipeDetected = false
                    isGestureHandled = false
                    isDragging = false
                    gestureDetector.onTouchEvent(event)
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.x - initialX)
                    val deltaY = abs(event.y - initialY)
                    
                    lastMoveX = event.x
                    lastMoveY = event.y

                    // Check if this is a drag gesture (very sensitive)
                    if (deltaX > minDragDistance || deltaY > minDragDistance) {
                        isSwipeDetected = true
                        isDragging = true
                    }

                    gestureDetector.onTouchEvent(event)
                }

                MotionEvent.ACTION_UP -> {
                    // Handle long press end
                    if (isLongPressing) {
                        isLongPressing = false
                        listener.onLongPressEnd()
                    }
                    
                    // Handle slow swipes based on distance (not just velocity)
                    // Only handle if gesture wasn't already handled by onFling
                    if (isSwipeDetected && !isGestureHandled && isDragging && !isLongPressing) {
                        val deltaX = event.x - initialX
                        val deltaY = event.y - initialY
                        val absDeltaX = abs(deltaX)
                        val absDeltaY = abs(deltaY)

                        // Use more lenient thresholds for slow swipes
                        val horizontalThreshold = shortSwipeThreshold
                        // For closing, require longer swipe (more protection)
                        val verticalCloseThreshold = closeSwipeThreshold

                        // Determine swipe direction with stricter rules
                        // For horizontal: X movement should be significantly more than Y
                        // For vertical (closing): Y movement should be significantly more than X AND X should be minimal
                        val isHorizontal = absDeltaX > absDeltaY * 1.5f  // 50% more horizontal (stricter)
                        val isVertical = absDeltaY > absDeltaX * 2.0f && absDeltaX < screenWidth * 0.1f  // Y must be 2x X AND X < 10% screen
                        
                        // Check if swipe distance is sufficient
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastGestureTime < minGestureInterval) {
                            // Too soon, ignore this gesture
                            resetState()
                            gestureDetector.onTouchEvent(event)
                            return@setOnTouchListener true
                        }
                        
                        if (isHorizontal && absDeltaX > horizontalThreshold) {
                            // Horizontal swipe - navigate between entries
                            if (deltaX > 0) {
                                // Swipe right - previous entry
                                executeSwipeAction { listener.onSwipeRight() }
                            } else {
                                // Swipe left - next entry
                                executeSwipeAction { listener.onSwipeLeft() }
                            }
                        } else if (isVertical && absDeltaY > verticalCloseThreshold && deltaY > 0) {
                            // Vertical swipe down - close stories (only if clearly vertical)
                            executeSwipeAction { listener.onSwipeDown() }
                        }
                        // If direction is unclear, ignore (prevents accidental closes)
                    }

                    // Reset all state flags after gesture completes
                    resetState()
                    gestureDetector.onTouchEvent(event)
                }

                MotionEvent.ACTION_CANCEL -> {
                    // Handle long press end on cancel
                    if (isLongPressing) {
                        isLongPressing = false
                        listener.onLongPressEnd()
                    }
                    // Reset all state flags on cancel
                    resetState()
                    gestureDetector.onTouchEvent(event)
                }

                else -> {
                    gestureDetector.onTouchEvent(event)
                }
            }
            true
        }
    }
    
    /**
     * Executes swipe action with debounce protection.
     * Prevents rapid consecutive swipes.
     */
    private fun executeSwipeAction(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        
        // Cancel any pending action
        pendingSwipeAction?.let { handler.removeCallbacks(it) }
        
        if (currentTime - lastGestureTime >= minGestureInterval) {
            // Enough time has passed, execute immediately
            lastGestureTime = currentTime
            action()
        } else {
            // Too soon, schedule for later
            val delay = minGestureInterval - (currentTime - lastGestureTime)
            pendingSwipeAction = {
                val now = System.currentTimeMillis()
                if (now - lastGestureTime >= minGestureInterval) {
                    lastGestureTime = now
                    action()
                }
                pendingSwipeAction = null
            }
            handler.postDelayed(pendingSwipeAction!!, delay)
        }
    }
    
    /**
     * Resets all gesture state flags.
     * Should be called after transitions complete to ensure clean state.
     */
    fun resetState() {
        // Cancel any pending actions
        pendingSwipeAction?.let { handler.removeCallbacks(it) }
        pendingSwipeAction = null
        
        isSwipeDetected = false
        isGestureHandled = false
        isDragging = false
        isLongPressing = false
        initialX = 0f
        initialY = 0f
        lastMoveX = 0f
        lastMoveY = 0f
    }
    
    /**
     * Cleans up all resources and cancels all pending operations.
     * Should be called when the handler is no longer needed (e.g., Activity.onDestroy).
     */
    fun cleanup() {
        // Remove all handler callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
        
        // Cancel any pending swipe actions
        pendingSwipeAction?.let { handler.removeCallbacks(it) }
        pendingSwipeAction = null
        
        // Remove touch listener from root view
        rootView.setOnTouchListener(null)
        
        // Reset all state
        resetState()
        
        // Clear callback reference
        isTransitioning = { false }
    }
}

