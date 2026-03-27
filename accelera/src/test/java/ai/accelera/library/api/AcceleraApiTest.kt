package ai.accelera.library.api

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.networking.HttpClient
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.networking.RequestMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class AcceleraApiTest {
    @Test
    fun `loadBanner uses content endpoint and authorization header`() {
        val fakeHttpClient = RecordingHttpClient()
        val api = AcceleraAPI(
            config = AcceleraConfig(url = "https://example.com", systemToken = "token"),
            httpClient = fakeHttpClient
        )

        api.loadBanner("""{"type":"stories"}""".toByteArray()) { _, _ -> }

        assertEquals("/api/v1/content", fakeHttpClient.lastPath)
        assertEquals(RequestMethod.POST, fakeHttpClient.lastMethod)
        assertEquals("token", fakeHttpClient.lastHeaders["Authorization"])
    }

    @Test
    fun `logEvent uses events endpoint`() {
        val fakeHttpClient = RecordingHttpClient()
        val api = AcceleraAPI(
            config = AcceleraConfig(url = "https://example.com"),
            httpClient = fakeHttpClient
        )

        api.logEvent("""{"event":"view"}""".toByteArray()) { _, _ -> }

        assertEquals("/api/v1/events", fakeHttpClient.lastPath)
        assertEquals(RequestMethod.POST, fakeHttpClient.lastMethod)
    }
}

private class RecordingHttpClient : HttpClient {
    var lastPath: String = ""
    var lastMethod: RequestMethod = RequestMethod.GET
    var lastHeaders: Map<String, String> = emptyMap()

    override fun execute(
        path: String,
        method: RequestMethod,
        body: ByteArray?,
        headers: Map<String, String>,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        lastPath = path
        lastMethod = method
        lastHeaders = headers
        completion(byteArrayOf(), null)
    }
}
