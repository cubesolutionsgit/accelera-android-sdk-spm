package ai.accelera.library.utils

import ai.accelera.library.core.constants.AcceleraJsonKeys
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.acceleraCardObjectOrNull(): JSONObject? {
    optJSONObject(AcceleraJsonKeys.CARD)?.let { return it }

    optJSONArray(AcceleraJsonKeys.CARDS)?.let { cards ->
        for (index in 0 until cards.length()) {
            cards.optJSONObject(index)?.let { return it }
        }
        return null
    }

    return this
}

internal fun JSONObject.normalizedDivDataObjectOrNull(): JSONObject? {
    val card = acceleraCardObjectOrNull() ?: return null

    if (card.has(AcceleraJsonKeys.STATES)) {
        return if (card.has(AcceleraJsonKeys.LOG_ID)) {
            card
        } else {
            JSONObject(card.toString()).put(AcceleraJsonKeys.LOG_ID, AcceleraJsonKeys.GENERATED_LOG_ID)
        }
    }

    val div = card.optJSONObject(AcceleraJsonKeys.DIV)
        ?: card.takeIf { it.has(AcceleraJsonKeys.TYPE) }
        ?: return null
    val logId = card.optString(AcceleraJsonKeys.LOG_ID).takeIf { it.isNotBlank() }
        ?: AcceleraJsonKeys.GENERATED_LOG_ID

    return JSONObject()
        .put(AcceleraJsonKeys.LOG_ID, logId)
        .put(
            AcceleraJsonKeys.STATES,
            JSONArray().put(
                JSONObject()
                    .put(AcceleraJsonKeys.STATE_ID, 0)
                    .put(AcceleraJsonKeys.DIV, div)
            )
        )
}

/**
 * JSON payload that asks the backend to return refreshed content.
 */
internal fun refreshPayloadJson(): String =
    JSONObject().put(AcceleraJsonKeys.REFRESH, true).toString()
