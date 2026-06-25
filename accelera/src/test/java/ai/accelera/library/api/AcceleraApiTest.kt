package ai.accelera.library.api

import ai.accelera.library.AcceleraConfig
import ai.accelera.library.networking.HttpClient
import ai.accelera.library.networking.NetworkError
import ai.accelera.library.networking.RequestMethod
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `request body is passed through unchanged`() {
        val fakeHttpClient = RecordingHttpClient()
        val api = AcceleraAPI(AcceleraConfig(url = "https://example.com"), fakeHttpClient)

        api.loadBanner("payload".toByteArray()) { _, _ -> }

        assertArrayEquals("payload".toByteArray(), fakeHttpClient.lastBody)
    }

    @Test
    fun `no authorization header when system token is absent`() {
        val fakeHttpClient = RecordingHttpClient()
        val api = AcceleraAPI(AcceleraConfig(url = "https://example.com"), fakeHttpClient)

        api.loadBanner(null) { _, _ -> }

        assertFalse(fakeHttpClient.lastHeaders.containsKey("Authorization"))
    }

    @Test
    fun `response and error are forwarded to completion`() {
        val response = RecordingHttpClient(response = "ok".toByteArray())
        val api = AcceleraAPI(AcceleraConfig(url = "https://example.com"), response)
        var data: ByteArray? = null
        api.loadBanner(null) { d, _ -> data = d }
        assertArrayEquals("ok".toByteArray(), data)

        val failing = RecordingHttpClient(error = NetworkError.Timeout)
        val api2 = AcceleraAPI(AcceleraConfig(url = "https://example.com"), failing)
        var error: NetworkError? = null
        api2.loadBanner(null) { _, e -> error = e }
        assertEquals(NetworkError.Timeout, error)
    }

    @Test
    fun `stub reports a not-implemented server error and no data`() {
        val stub = AcceleraAPIStub()
        var data: ByteArray? = ByteArray(0)
        var error: NetworkError? = null
        stub.loadBanner(null) { d, e -> data = d; error = e }

        assertNull(data)
        assertTrue(error is NetworkError.Server)
    }
}

private class RecordingHttpClient(
    private val response: ByteArray? = byteArrayOf(),
    private val error: NetworkError? = null
) : HttpClient {
    var lastPath: String = ""
    var lastMethod: RequestMethod = RequestMethod.GET
    var lastHeaders: Map<String, String> = emptyMap()
    var lastBody: ByteArray? = null

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
        lastBody = body
        completion(response, error)
    }
}
