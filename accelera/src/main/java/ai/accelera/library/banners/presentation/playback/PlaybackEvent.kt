package ai.accelera.library.banners.presentation.playback

sealed class PlaybackEvent {
    data class Open(val entryId: String) : PlaybackEvent()
    object TapNext : PlaybackEvent()
    object TapPrev : PlaybackEvent()
    object SwipeNextEntry : PlaybackEvent()
    object SwipePrevEntry : PlaybackEvent()
    object LongPressStart : PlaybackEvent()
    object LongPressEnd : PlaybackEvent()
    object ActivityPause : PlaybackEvent()
    object ActivityResume : PlaybackEvent()
    object CloseRequested : PlaybackEvent()
    object Destroyed : PlaybackEvent()
}

