package ai.accelera.library.api.internal

interface ApiEndpointResolver {
    fun contentPath(): String
    fun eventsPath(): String
}

class DefaultApiEndpointResolver : ApiEndpointResolver {
    override fun contentPath(): String = "/api/v1/content"
    override fun eventsPath(): String = "/api/v1/events"
}
