package ai.accelera.library.banners.domain.logging

import org.json.JSONObject

interface StoryAnalytics {
    fun logCardView(meta: JSONObject)
    fun logError(message: String)
}
