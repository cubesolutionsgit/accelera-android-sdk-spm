package ai.accelera.library.banners.data.repository

import ai.accelera.library.Accelera
import org.json.JSONObject

/**
 * Repository for managing story data parsing and extraction.
 */
class StoryDataRepository(private val jsonData: ByteArray) {

    /**
     * Loads all entry IDs from the JSON data.
     */
    fun loadEntryIds(): List<String> {
        return try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject("fullscreens")
            if (fullscreens != null) {
                fullscreens.keys().asSequence().sorted().toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Accelera.shared.error("Failed to load entry IDs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Loads cards for a specific entry ID.
     */
    fun loadEntryCards(entryId: String): List<JSONObject>? {
        return try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject("fullscreens")
            val entry = fullscreens?.optJSONObject(entryId) ?: return null

            val cardsArray = entry.optJSONArray("cards")
            if (cardsArray != null) {
                (0 until cardsArray.length()).map { cardsArray.getJSONObject(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Accelera.shared.error("Failed to load entry cards: ${e.message}")
            null
        }
    }

    /**
     * Extracts duration from a card JSON object.
     */
    fun getCardDuration(card: JSONObject): Long {
        val cardObj = card.optJSONObject("card")
        val duration = cardObj?.optInt("duration") ?: card.optInt("duration", 0)
        return if (duration > 0) duration.toLong() else 5000L // Default 5 seconds
    }

    /**
     * Checks if a card has a duration.
     */
    fun hasDuration(card: JSONObject): Boolean {
        val cardObj = card.optJSONObject("card")
        val duration = cardObj?.optInt("duration") ?: card.optInt("duration")
        return duration > 0
    }

    /**
     * Extracts meta information from a card for logging.
     */
    fun getCardMeta(card: JSONObject): JSONObject {
        return card.optJSONObject("card")?.optJSONObject("meta") ?: JSONObject()
    }
}
