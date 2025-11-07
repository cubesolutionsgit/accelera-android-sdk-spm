package ai.accelera.library.utils

import org.json.JSONObject

/**
 * Extension functions for ByteArray to extract card data (similar to Data extensions in iOS).
 */
val ByteArray.closable: Boolean?
    get() = extractValue<Boolean>("closable")

val ByteArray.duration: Int?
    get() = extractValue<Int>("duration")

val ByteArray.meta: Any?
    get() = extractValue<Any>("meta")

private inline fun <reified T> ByteArray.extractValue(key: String): T? {
    return try {
        val jsonString = String(this, Charsets.UTF_8)
        val jsonObject = JSONObject(jsonString)
        val root = jsonObject.optJSONObject("card") ?: return null
        root.opt(key) as? T
    } catch (e: Exception) {
        null
    }
}

