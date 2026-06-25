package ai.accelera.library.core.constants

/**
 * UI dimensions in density-independent pixels (dp) unless suffixed `_PX`.
 * Convert with `Context.dpToPx` / `dpToPxF`.
 */
internal object AcceleraDimens {
    const val CLOSE_BUTTON_SIZE_DP = 24
    const val CLOSE_BUTTON_CORNER_RADIUS_PX = 12f
    const val CLOSE_BUTTON_X_HALF_LENGTH_DP = 8f
    const val CLOSE_BUTTON_STROKE_WIDTH_PX = 3f

    const val CONTENT_CLOSE_MARGIN_DP = 8
    const val POPUP_CLOSE_TOP_MARGIN_DP = 28
    const val POPUP_CLOSE_END_MARGIN_DP = 16
    const val FULLSCREEN_CLOSE_TOP_MARGIN_DP = 24
    const val FULLSCREEN_CLOSE_END_MARGIN_DP = 16

    const val PROGRESS_TOP_MARGIN_DP = 4
    const val PROGRESS_SIDE_MARGIN_DP = 8
    const val PROGRESS_BAR_HEIGHT_DP = 2
    const val PROGRESS_BAR_SPACING_DP = 4

    const val PROGRESS_CONTAINER_ELEVATION = 10f
    const val CLOSE_BUTTON_ELEVATION = 20f
}

/**
 * Animation, timer and timeout durations in milliseconds (unless suffixed otherwise).
 */
internal object AcceleraTiming {
    const val ENTRY_ANIMATION_MS = 300L
    const val CARD_FADE_MS = 200L
    const val PROGRESS_FRAME_INTERVAL_MS = 16L
    const val STATE_SETTLE_DELAY_MS = 40L
    const val GESTURE_DEBOUNCE_MS = 150L
    const val DEFAULT_CARD_DURATION_MS = 5000L
    const val TRANSITION_DEBOUNCE_MS = 200L
}

/**
 * Alpha channel values (0..255) for overlay colors.
 */
internal object AcceleraColors {
    /** ~30% white track behind the progress fill. */
    const val PROGRESS_TRACK_ALPHA = 76

    /** ~20% black scrim behind the close button. */
    const val CLOSE_BUTTON_BG_ALPHA = 51
}

/**
 * Story gesture detection thresholds. `_DP` values are scaled by display density;
 * `_RATIO` values are fractions of screen width/height.
 */
internal object AcceleraGestureConfig {
    const val SWIPE_THRESHOLD_DP = 50f
    const val MIN_SWIPE_VELOCITY_DP = 300f
    const val MIN_DRAG_DISTANCE_DP = 5f
    const val SHORT_SWIPE_SCREEN_RATIO = 0.15f
    const val SHORT_SWIPE_VELOCITY_DP = 200f
    const val CLOSE_SWIPE_SCREEN_RATIO = 0.15f
    const val CLOSE_SWIPE_VELOCITY_DP = 300f

    /** Horizontal swipe requires |dx| > |dy| * this. */
    const val HORIZONTAL_DOMINANCE_RATIO = 1.5f

    /** Vertical (close) swipe requires |dy| > |dx| * this. */
    const val VERTICAL_DOMINANCE_RATIO = 2.0f

    /** Vertical swipe also requires |dx| < screenWidth * this. */
    const val VERTICAL_MAX_X_SCREEN_RATIO = 0.1f

    /** Tap navigation splits the width into this many zones (left/middle/right). */
    const val TAP_ZONE_COUNT = 3f

    const val CLOSE_BUTTON_SIZE_DP = 24f
    const val CLOSE_BUTTON_MARGIN_DP = 16f
}
