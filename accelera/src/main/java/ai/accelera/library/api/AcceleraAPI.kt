package ai.accelera.library.api

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.api.internal.ApiEndpointResolver
import ai.accelera.library.api.internal.DefaultApiEndpointResolver
import ai.accelera.library.networking.HttpClient
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.networking.RequestMethod
import ai.accelera.library.networking.WebClient

/**
 * Default implementation of [AcceleraAPIProtocol] using [WebClient].
 * Similar to iOS implementation - uses URL as-is if it contains path, otherwise appends default path.
 * If URL already contains query parameters, uses GET method, otherwise uses POST.
 */
class AcceleraAPI(
    private val config: AcceleraConfig,
    private val httpClient: HttpClient = WebClient(config.url ?: ""),
    private val endpointResolver: ApiEndpointResolver = DefaultApiEndpointResolver()
) : AcceleraAPIProtocol {

    override fun logEvent(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        val headers = mutableMapOf<String, String>()
        config.systemToken?.let {
            headers["Authorization"] = it
        }

        // Always use POST for events
        httpClient.execute(
            path = endpointResolver.eventsPath(),
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

        httpClient.execute(
            path = endpointResolver.contentPath(),
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
