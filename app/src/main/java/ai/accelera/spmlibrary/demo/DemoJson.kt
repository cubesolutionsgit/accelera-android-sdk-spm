package ai.accelera.spmlibrary.demo

import ai.accelera.library.Accelera
import ai.accelera.library.AcceleraConfig
import org.json.JSONObject

internal const val defaultStoriesJson = """
{
  "type": "stories",
  "slot": "story",
  "channel": "test_channel"
}
"""

internal const val defaultBannerJson = """
{
  "type": "banner",
  "slot": "messages_top_banner",
  "channel": "dev"
}
"""

internal const val defaultPopupJson = """
{
  "type": "popup",
  "slot": "main_popup",
  "channel": "dev"
}
"""

internal const val defaultUserInfoJson = """
{
  "client_id": "android-demo-user",
  "language": "KZ",
  "segment": "premium"
}
"""

internal fun configureAccelera(url: String, token: String) {
    Accelera.shared.configure(
        config = AcceleraConfig(
            url = url,
            systemToken = token
        )
    )
}

internal fun validateJson(text: String, showError: (String) -> Unit): ByteArray? {
    return runCatching {
        JSONObject(text).toString().toByteArray(Charsets.UTF_8)
    }.getOrElse { error ->
        showError("Invalid JSON: ${error.message}")
        null
    }
}

internal fun jsonToMapOrNull(text: String): Map<String, Any?>? {
    return runCatching {
        JSONObject(text).toMap()
    }.getOrNull()
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = get(key)
        result[key] = when (value) {
            JSONObject.NULL -> null
            is JSONObject -> value.toMap()
            else -> value
        }
    }
    return result
}

