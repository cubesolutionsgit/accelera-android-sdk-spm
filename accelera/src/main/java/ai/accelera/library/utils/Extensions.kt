package ai.accelera.library.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Converts a Map to JSON ByteArray.
 */
fun Map<String, Any?>.toJsonBytes(): ByteArray {
    return try {
        val jsonObject = JSONObject(this)
        jsonObject.toString().toByteArray(Charsets.UTF_8)
    } catch (e: Exception) {
        byteArrayOf()
    }
}

/**
 * Converts a Map to JSON String.
 */
fun Map<String, Any?>.toJsonString(): String {
    return try {
        val jsonObject = JSONObject(this)
        jsonObject.toString()
    } catch (e: Exception) {
        "{}"
    }
}

/**
 * Merges two JSON strings, with new values taking precedence.
 */
fun mergeJSON(old: String?, new: String?): String? {
    if (old == null) return new
    if (new == null) return old

    return try {
        val oldJson = JSONObject(old)
        val newJson = JSONObject(new)
        
        val keys = newJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            oldJson.put(key, newJson.get(key))
        }
        
        oldJson.toString()
    } catch (e: Exception) {
        new
    }
}

