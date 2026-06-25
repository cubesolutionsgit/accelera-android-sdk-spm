package ai.accelera.library.banners.data.repository

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.repository.StoryDataSource
import ai.accelera.library.core.constants.AcceleraJsonKeys
import ai.accelera.library.core.constants.AcceleraTiming
import org.json.JSONObject

/**
 * Repository for managing story data parsing and extraction.
 */
class StoryDataRepository(private val jsonData: ByteArray) : StoryDataSource {

    /**
     * Loads all entry IDs from the JSON data.
     */
    override fun loadEntryIds(): List<String> {
        return try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject(AcceleraJsonKeys.FULLSCREENS)
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
    override fun loadEntryCards(entryId: String): List<JSONObject>? {
        return try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject(AcceleraJsonKeys.FULLSCREENS)
            val entry = fullscreens?.optJSONObject(entryId) ?: return null

            val cardsArray = entry.optJSONArray(AcceleraJsonKeys.CARDS)
            if (cardsArray != null) {
                (0 until cardsArray.length()).mapNotNull { cardsArray.optJSONObject(it) }
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
    override fun getCardDuration(card: JSONObject): Long {
        val cardObj = card.optJSONObject(AcceleraJsonKeys.CARD)
        val duration = cardObj?.optInt(AcceleraJsonKeys.DURATION) ?: card.optInt(AcceleraJsonKeys.DURATION, 0)
        return if (duration > 0) duration.toLong() else AcceleraTiming.DEFAULT_CARD_DURATION_MS
    }

    /**
     * Checks if a card has a duration.
     */
    override fun hasDuration(card: JSONObject): Boolean {
        val cardObj = card.optJSONObject(AcceleraJsonKeys.CARD)
        val duration = cardObj?.optInt(AcceleraJsonKeys.DURATION) ?: card.optInt(AcceleraJsonKeys.DURATION)
        return duration > 0
    }

    /**
     * Extracts meta information from a card for logging.
     */
    override fun getCardMeta(card: JSONObject): JSONObject {
        return card.optJSONObject(AcceleraJsonKeys.CARD)?.optJSONObject(AcceleraJsonKeys.META) ?: JSONObject()
    }
}
