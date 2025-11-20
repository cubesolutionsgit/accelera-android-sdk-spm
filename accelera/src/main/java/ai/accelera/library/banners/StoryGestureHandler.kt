package ai.accelera.library.banners

import android.content.Context
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
 * Supports taps and swipes.
 */
class StoryGestureHandler(
    context: Context,
    private val rootView: ViewGroup,
    private val listener: StoryGestureListener
) {
    private val swipeThreshold = 100 * context.resources.displayMetrics.density
    private val minSwipeVelocity = 500 * context.resources.displayMetrics.density
    private val minDragDistance = 10 * context.resources.displayMetrics.density

    private var initialX = 0f
    private var initialY = 0f
    private var isSwipeDetected = false
    private var isGestureHandled = false
    private var isDragging = false
    private var isLongPressing = false

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isLongPressing = true
                listener.onLongPress()
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Don't handle tap if we were dragging
                if (isDragging) {
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

                // Determine if tap is on left or right half
                val isRightHalf = tapX > screenWidth / 2

                if (isRightHalf) {
                    listener.onTapRight()
                } else {
                    listener.onTapLeft()
                }

                return true
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

                // Determine if horizontal or vertical swipe
                if (absDeltaX > absDeltaY) {
                    // Horizontal swipe - navigate between entries
                    if (absDeltaX > swipeThreshold && abs(velocityX) > minSwipeVelocity) {
                        if (deltaX > 0) {
                            // Swipe right - previous entry
                            listener.onSwipeRight()
                        } else {
                            // Swipe left - next entry
                            listener.onSwipeLeft()
                        }
                        isGestureHandled = true
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (absDeltaY > swipeThreshold && abs(velocityY) > minSwipeVelocity) {
                        if (deltaY > 0 && velocityY > minSwipeVelocity) {
                            // Swipe down - close stories
                            listener.onSwipeDown()
                            isGestureHandled = true
                            return true
                        }
                    }
                }

                return false
            }
        }
    )

    /**
     * Sets up touch listener on the root view.
     */
    fun setupTouchListener() {
        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
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

                    // Check if this is a drag gesture
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

                        // Check if swipe distance is sufficient
                        if (absDeltaX > swipeThreshold || absDeltaY > swipeThreshold) {
                            // Determine if horizontal or vertical swipe
                            if (absDeltaX > absDeltaY) {
                                // Horizontal swipe - navigate between entries
                                if (deltaX > 0) {
                                    // Swipe right - previous entry
                                    listener.onSwipeRight()
                                } else {
                                    // Swipe left - next entry
                                    listener.onSwipeLeft()
                                }
                            } else {
                                // Vertical swipe down - close stories
                                if (deltaY > 0 && absDeltaY > swipeThreshold) {
                                    listener.onSwipeDown()
                                }
                            }
                        }
                    }

                    isDragging = false
                    gestureDetector.onTouchEvent(event)
                }

                MotionEvent.ACTION_CANCEL -> {
                    // Handle long press end on cancel
                    if (isLongPressing) {
                        isLongPressing = false
                        listener.onLongPressEnd()
                    }
                    isDragging = false
                    gestureDetector.onTouchEvent(event)
                }

                else -> {
                    gestureDetector.onTouchEvent(event)
                }
            }
            true
        }
    }
}

