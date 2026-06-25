package ai.accelera.library.core.constants

/**
 * HTTP transport configuration shared by the networking layer.
 */
internal object AcceleraHttp {
    const val TIMEOUT_SECONDS = 30L
    const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"
    const val HEADER_AUTHORIZATION = "Authorization"
}

/**
 * REST endpoint paths appended to the configured base URL.
 */
internal object AcceleraEndpoints {
    const val CONTENT = "/api/v1/content"
    const val EVENTS = "/api/v1/events"
}
