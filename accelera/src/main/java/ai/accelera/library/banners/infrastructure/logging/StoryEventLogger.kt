package ai.accelera.library.banners.infrastructure.logging

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.logging.StoryAnalytics
import ai.accelera.library.utils.toJsonBytes
import org.json.JSONObject

/**
 * Handles event logging for story cards.
 * Centralizes event logging logic to avoid code duplication (DRY).
 * 
 * Follows Single Responsibility Principle - only handles event logging.
 */
class StoryEventLogger(
    private val analytics: StoryAnalytics = AcceleraStoryAnalytics()
) {
    /**
     * Logs a view event for a specific card.
     * 
     * @param card The card JSON object to log view event for
     */
    fun logCardView(card: JSONObject) {
        try {
            val meta = card.optJSONObject("card")?.optJSONObject("meta") ?: JSONObject()
            analytics.logCardView(meta)
        } catch (e: Exception) {
            analytics.logError("Error logging view event: ${e.message}")
        }
    }
}

class AcceleraStoryAnalytics : StoryAnalytics {
    override fun logCardView(meta: JSONObject) {
        val eventPayload = mapOf(
            "event" to "view",
            "meta" to meta.toString()
        )
        Accelera.shared.logEvent(eventPayload.toJsonBytes())
    }

    override fun logError(message: String) {
        Accelera.shared.error(message)
    }
}

