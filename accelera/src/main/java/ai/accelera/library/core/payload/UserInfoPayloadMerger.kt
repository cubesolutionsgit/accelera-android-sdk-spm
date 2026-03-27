package ai.accelera.library.core.payload

import ai.accelera.library.utils.toJsonBytes
import org.json.JSONObject

interface PayloadMerger {
    fun mergeUserInfo(payload: ByteArray?, userInfo: String?): ByteArray?
}

class JsonUserInfoPayloadMerger : PayloadMerger {
    override fun mergeUserInfo(payload: ByteArray?, userInfo: String?): ByteArray? {
        val merged = mutableMapOf<String, Any?>()
        if (payload != null) {
            runCatching {
                val body = JSONObject(String(payload, Charsets.UTF_8))
                val keys = body.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    merged[key] = body.get(key)
                }
            }
        }

        if (!userInfo.isNullOrBlank()) {
            runCatching { merged["userInfo"] = JSONObject(userInfo) }
        }

        return merged.toJsonBytes()
    }
}
