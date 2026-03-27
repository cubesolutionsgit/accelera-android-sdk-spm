package ai.accelera.library.api.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultApiEndpointResolverTest {
    @Test
    fun `returns expected endpoint paths`() {
        val resolver = DefaultApiEndpointResolver()
        assertEquals("/api/v1/content", resolver.contentPath())
        assertEquals("/api/v1/events", resolver.eventsPath())
    }
}
