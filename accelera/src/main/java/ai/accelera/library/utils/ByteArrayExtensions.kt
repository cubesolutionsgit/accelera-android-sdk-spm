package ai.accelera.library.utils

import ai.accelera.library.core.constants.AcceleraJsonKeys
import org.json.JSONObject

/**
 * Extension functions for ByteArray to extract card data (similar to Data extensions in iOS).
 */
val ByteArray.closable: Boolean?
    get() = extractValue<Boolean>(AcceleraJsonKeys.CLOSABLE)

val ByteArray.duration: Int?
    get() = extractValue<Int>(AcceleraJsonKeys.DURATION)

val ByteArray.meta: Any?
    get() = extractValue<Any>(AcceleraJsonKeys.META)

private inline fun <reified T> ByteArray.extractValue(key: String): T? {
    return try {
        val jsonString = String(this, Charsets.UTF_8)
        val jsonObject = JSONObject(jsonString)
        val root = jsonObject.acceleraCardObjectOrNull() ?: return null
        root.opt(key) as? T
    } catch (e: Exception) {
        null
    }
}

