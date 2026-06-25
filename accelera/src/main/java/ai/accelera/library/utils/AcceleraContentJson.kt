package ai.accelera.library.utils

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.acceleraCardObjectOrNull(): JSONObject? {
    optJSONObject("card")?.let { return it }

    optJSONArray("cards")?.let { cards ->
        for (index in 0 until cards.length()) {
            cards.optJSONObject(index)?.let { return it }
        }
        return null
    }

    return this
}

internal fun JSONObject.normalizedDivDataObjectOrNull(): JSONObject? {
    val card = acceleraCardObjectOrNull() ?: return null

    if (card.has("states")) {
        return if (card.has("log_id")) {
            card
        } else {
            JSONObject(card.toString()).put("log_id", GENERATED_LOG_ID)
        }
    }

    val div = card.optJSONObject("div") ?: card.takeIf { it.has("type") } ?: return null
    val logId = card.optString("log_id").takeIf { it.isNotBlank() } ?: GENERATED_LOG_ID

    return JSONObject()
        .put("log_id", logId)
        .put(
            "states",
            JSONArray().put(
                JSONObject()
                    .put("state_id", 0)
                    .put("div", div)
            )
        )
}

private const val GENERATED_LOG_ID = "accelera_generated"
