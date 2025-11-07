package ai.accelera.library.api

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.networking.RequestMethod
import ai.accelera.library.networking.WebClient
import java.net.URL

/**
 * Default implementation of [AcceleraAPIProtocol] using [WebClient].
 * Similar to iOS implementation - uses URL as-is if it contains path, otherwise appends default path.
 * If URL already contains query parameters, uses GET method, otherwise uses POST.
 */
class AcceleraAPI(private val config: AcceleraConfig) : AcceleraAPIProtocol {
    private val baseUrl: String = config.url ?: ""
    
    /**
     * Creates WebClient with base URL.
     * If URL already contains a path, it will be used as-is (empty path in load()).
     * Otherwise, default paths will be appended.
     */
    private val client: WebClient = WebClient(baseUrl)

    /**
     * Checks if URL already contains a path (not just domain).
     * Returns true if URL has path after domain (e.g., "http://example.com/form").
     */
    private fun urlHasPath(url: String): Boolean {
        if (url.isEmpty()) return false
        
        return try {
            val urlObj = URL(url)
            urlObj.path.isNotEmpty() || urlObj.query != null
        } catch (e: Exception) {
            // If URL parsing fails, check manually
            val protocolEnd = url.indexOf("://")
            if (protocolEnd == -1) return false
            
            val afterProtocol = url.substring(protocolEnd + 3)
            val pathStart = afterProtocol.indexOf("/")
            pathStart != -1 && pathStart < afterProtocol.length - 1
        }
    }

    /**
     * Checks if URL contains query parameters.
     */
    private fun urlHasQuery(url: String): Boolean {
        if (url.isEmpty()) return false
        
        return try {
            val urlObj = URL(url)
            urlObj.query != null
        } catch (e: Exception) {
            // If URL parsing fails, check manually
            url.contains("?")
        }
    }

    override fun logEvent(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        val headers = mutableMapOf<String, String>()
        config.systemToken?.let {
            headers["Authorization"] = it
        }

        // Always use POST for events
        client.load(
            path = "/api/v1/events",
            method = RequestMethod.POST,
            body = data,
            headers = headers,
            completion = completion
        )
    }

    override fun loadBanner(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        val headers = mutableMapOf<String, String>()
        config.systemToken?.let {
            headers["Authorization"] = it
        }

        client.load(
            path = "/api/v1/content",
            method = RequestMethod.POST,
            body = data,
            headers = headers,
            completion = completion
        )
    }
}

/**
 * Stub implementation of [AcceleraAPIProtocol] for cases when API is not configured.
 */
class AcceleraAPIStub : AcceleraAPIProtocol {
    override fun logEvent(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        completion(null, NetworkError.Server(501, "logEvent is not implemented in stub"))
    }

    override fun loadBanner(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        completion(null, NetworkError.Server(501, "loadBanner is not implemented in stub"))
    }
}
