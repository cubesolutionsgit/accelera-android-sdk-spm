package ai.accelera.library.core.events

import org.json.JSONObject

interface EventActionExtractor {
    fun extract(eventData: ByteArray): String?
}

class JsonEventActionExtractor : EventActionExtractor {
    override fun extract(eventData: ByteArray): String? {
        return runCatching {
            JSONObject(String(eventData, Charsets.UTF_8)).optString("event").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
