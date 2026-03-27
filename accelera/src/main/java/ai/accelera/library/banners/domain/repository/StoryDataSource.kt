package ai.accelera.library.banners.domain.repository

import org.json.JSONObject

interface StoryDataSource {
    fun loadEntryIds(): List<String>
    fun loadEntryCards(entryId: String): List<JSONObject>?
    fun getCardDuration(card: JSONObject): Long
    fun hasDuration(card: JSONObject): Boolean
    fun getCardMeta(card: JSONObject): JSONObject
}
