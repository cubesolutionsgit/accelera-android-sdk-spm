package ai.accelera.library.api

import ai.accelera.library.networking.NetworkError

/**
 * Protocol for all network interactions performed by Accelera.
 * You can override this protocol via [AcceleraDelegate.customAPI].
 */
interface AcceleraAPIProtocol {
    /**
     * Sends user event analytics to backend.
     * @param data JSON data of the event.
     * @param completion Completion callback with response data or error.
     */
    fun logEvent(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    )

    /**
     * Loads remote banner configuration or data from backend.
     * @param data Optional JSON payload to send with the request.
     * @param completion Completion callback with response data or error.
     */
    fun loadBanner(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    )
}

