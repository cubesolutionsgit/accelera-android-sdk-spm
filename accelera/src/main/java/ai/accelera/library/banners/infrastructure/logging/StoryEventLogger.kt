package ai.accelera.library.banners.infrastructure.logging

import ai.accelera.library.Accelera
import ai.accelera.library.utils.toJsonBytes
import ai.accelera.library.banners.data.repository.StoryDataRepository
import org.json.JSONObject

/**
 * Handles event logging for story cards.
 * Centralizes event logging logic to avoid code duplication (DRY).
 * 
 * Follows Single Responsibility Principle - only handles event logging.
 */
class StoryEventLogger(
    private val dataRepository: StoryDataRepository
) {
    /**
     * Logs a view event for a specific card.
     * 
     * @param card The card JSON object to log view event for
     */
    fun logCardView(card: JSONObject) {
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
    }
}

