package ai.accelera.library.banners.infrastructure.cache

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process registry for handing JSON payloads to [ai.accelera.library.banners.presentation.ui.FullscreenActivity]
 * and [ai.accelera.library.banners.presentation.ui.PopupActivity].
 *
 * Payloads can reach hundreds of KB, so passing them as Intent extras risks
 * TransactionTooLargeException (~1MB Binder limit). Instead the payload stays in process
 * memory and only a short token travels through the Intent. After process death the token
 * resolves to nothing and the activity closes gracefully.
 */
internal object AcceleraPayloadRegistry {
    private val payloads = ConcurrentHashMap<String, ByteArray>()

    fun register(payload: ByteArray): String {
        val token = UUID.randomUUID().toString()
        payloads[token] = payload
        return token
    }

    fun get(token: String?): ByteArray? {
        if (token.isNullOrBlank()) return null
        return payloads[token]
    }

    fun remove(token: String?) {
        if (!token.isNullOrBlank()) {
            payloads.remove(token)
        }
    }
}
