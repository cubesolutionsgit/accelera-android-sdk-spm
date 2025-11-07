package ai.accelera.library.networking

/**
 * Represents networking-related errors that may occur during a request.
 */
sealed class NetworkError : Exception() {
    /**
     * No internet connection available.
     */
    object NoConnection : NetworkError()

    /**
     * The request timed out.
     */
    object Timeout : NetworkError()

    /**
     * The request was cancelled before completion.
     */
    object Cancelled : NetworkError()

    /**
     * The response could not be decoded.
     */
    object Decoding : NetworkError()

    /**
     * A response was received, but it was not a valid HTTP response.
     */
    object BadResponse : NetworkError()

    /**
     * Server responded with an error HTTP status code (4xx or 5xx).
     * @param status HTTP status code returned by the server.
     * @param message Optional message from the server response body.
     */
    data class Server(val status: Int, val errorMessage: String?) : NetworkError() {
        override val message: String?
            get() = errorMessage ?: "Server error (code $status)"
    }

    /**
     * A low-level networking error occurred.
     * @param error The underlying system error.
     */
    data class InternalError(val error: Throwable) : NetworkError() {
        override val message: String?
            get() = error.message ?: "Internal error"
    }

    override val message: String?
        get() = when (this) {
            is NoConnection -> "No internet connection"
            is Timeout -> "Request timed out"
            is Cancelled -> "Request was cancelled"
            is Decoding -> "Failed to decode response"
            is BadResponse -> "Invalid or missing HTTP response"
            is Server -> this.message
            is InternalError -> this.message
        }
}

