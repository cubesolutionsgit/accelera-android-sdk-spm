package ai.accelera.library.api.internal

import ai.accelera.library.core.constants.AcceleraEndpoints

interface ApiEndpointResolver {
    fun contentPath(): String
    fun eventsPath(): String
}

class DefaultApiEndpointResolver : ApiEndpointResolver {
    override fun contentPath(): String = AcceleraEndpoints.CONTENT
    override fun eventsPath(): String = AcceleraEndpoints.EVENTS
}
