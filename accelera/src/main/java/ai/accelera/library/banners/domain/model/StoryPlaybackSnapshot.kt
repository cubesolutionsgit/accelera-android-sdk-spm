package ai.accelera.library.banners.domain.model

/**
 * Point-in-time playback position of a stories screen, used to resume seamlessly
 * after the hosting activity is recreated (rotation, theme change, etc.).
 *
 * @property entryId Entry (story) that was showing.
 * @property cardIndex Index of the card inside the entry.
 * @property progressFraction Elapsed fraction (0..1) of the card's timer.
 * @property videoPositionsMs Playback positions of the card's video/audio players,
 * in player creation order (deterministic for identical card JSON).
 */
data class StoryPlaybackSnapshot(
    val entryId: String,
    val cardIndex: Int,
    val progressFraction: Float,
    val videoPositionsMs: List<Long> = emptyList()
)
