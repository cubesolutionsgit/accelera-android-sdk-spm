package ai.accelera.library.banners.infrastructure.cache

import android.util.LruCache

/**
 * Process-wide in-memory cache for loaded banner/story JSON payloads, keyed by request key.
 *
 * Payloads can reach hundreds of KB, so they must never go into saved instance state
 * (that overflows the ~1MB Binder transaction limit and crashes with
 * TransactionTooLargeException). This cache lets a banner survive configuration changes
 * and lazy-list recycling without a network re-fetch; after process death content is
 * simply reloaded.
 */
internal object BannerContentCache {

    private const val MAX_SIZE_BYTES = 4 * 1024 * 1024

    private val cache = object : LruCache<String, ByteArray>(MAX_SIZE_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun get(key: String): ByteArray? = cache.get(key)

    fun put(key: String, value: ByteArray) {
        cache.put(key, value)
    }

    fun remove(key: String) {
        cache.remove(key)
    }
}
