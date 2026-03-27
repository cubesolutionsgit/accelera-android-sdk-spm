package ai.accelera.library.core.events

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parsed action payload extracted from event body.
 */
data class EventActionPayload(
    val actionName: String,
    val params: Map<String, String>,
    val meta: Any?
)

interface EventActionPayloadExtractor {
    fun extract(eventData: ByteArray): EventActionPayload?
}

class JsonEventActionPayloadExtractor : EventActionPayloadExtractor {
    override fun extract(eventData: ByteArray): EventActionPayload? {
        return runCatching {
            val json = JSONObject(String(eventData, Charsets.UTF_8))
            val actionName = json.optString("event").takeIf { it.isNotBlank() } ?: return null
            val params = json.optJSONObject("params").toStringMap()
            val meta = normalizeJsonValue(json.opt("meta"))
            EventActionPayload(actionName = actionName, params = params, meta = meta)
        }.getOrNull()
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) return emptyMap()
        val result = mutableMapOf<String, String>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = opt(key)?.takeUnless { it == JSONObject.NULL }?.toString() ?: ""
        }
        return result
    }

    private fun normalizeJsonValue(value: Any?): Any? {
        return when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> {
                buildMap {
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        put(key, normalizeJsonValue(value.opt(key)))
                    }
                }
            }
            is JSONArray -> {
                buildList {
                    for (i in 0 until value.length()) {
                        add(normalizeJsonValue(value.opt(i)))
                    }
                }
            }
            else -> value
        }
    }
}
