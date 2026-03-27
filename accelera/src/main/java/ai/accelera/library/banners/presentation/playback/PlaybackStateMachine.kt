package ai.accelera.library.banners.presentation.playback

import ai.accelera.library.Accelera

enum class PlaybackState {
    Idle,
    PreparingEntry,
    ShowingCard,
    TransitioningEntry,
    PausedByLifecycle,
    PausedByGesture,
    Closing,
    Destroyed
}

fun interface StoriesStateLogger {
    fun log(message: String)
}

class PlaybackStateMachine {
    constructor() : this(StoriesStateLogger { Accelera.shared.log(it) })
    constructor(logger: StoriesStateLogger) {
        this.logger = logger
    }

    private val logger: StoriesStateLogger

    var state: PlaybackState = PlaybackState.Idle
        private set

    fun onEvent(event: PlaybackEvent): Boolean {
        val next = nextState(state, event) ?: return false
        if (next != state) {
            logger.log("Stories state: $state -> $event -> $next")
        }
        state = next
        return true
    }

    fun forceState(newState: PlaybackState) {
        if (state != newState) {
            logger.log("Stories state force: $state -> $newState")
            state = newState
        }
    }

    private fun nextState(current: PlaybackState, event: PlaybackEvent): PlaybackState? {
        return when (current) {
            PlaybackState.Idle -> when (event) {
                is PlaybackEvent.Open -> PlaybackState.PreparingEntry
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.PreparingEntry -> when (event) {
                PlaybackEvent.ActivityPause -> PlaybackState.PausedByLifecycle
                PlaybackEvent.CloseRequested -> PlaybackState.Closing
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> current
            }

            PlaybackState.ShowingCard -> when (event) {
                PlaybackEvent.TapNext,
                PlaybackEvent.TapPrev -> current
                PlaybackEvent.SwipeNextEntry,
                PlaybackEvent.SwipePrevEntry -> PlaybackState.TransitioningEntry
                PlaybackEvent.LongPressStart -> PlaybackState.PausedByGesture
                PlaybackEvent.ActivityPause -> PlaybackState.PausedByLifecycle
                PlaybackEvent.CloseRequested -> PlaybackState.Closing
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.TransitioningEntry -> when (event) {
                PlaybackEvent.ActivityPause -> PlaybackState.PausedByLifecycle
                PlaybackEvent.CloseRequested -> PlaybackState.Closing
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.PausedByLifecycle -> when (event) {
                PlaybackEvent.ActivityResume -> PlaybackState.ShowingCard
                PlaybackEvent.CloseRequested -> PlaybackState.Closing
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.PausedByGesture -> when (event) {
                PlaybackEvent.LongPressEnd -> PlaybackState.ShowingCard
                PlaybackEvent.ActivityPause -> PlaybackState.PausedByLifecycle
                PlaybackEvent.CloseRequested -> PlaybackState.Closing
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.Closing -> when (event) {
                PlaybackEvent.Destroyed -> PlaybackState.Destroyed
                else -> null
            }

            PlaybackState.Destroyed -> null
        }
    }
}

