package ai.accelera.library.core.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventActionExtractorTest {

    private val extractor = JsonEventActionPayloadExtractor()

    @Test
    fun `extracts event name params and meta`() {
        val data = """
            {"event":"fullscreen","params":{"id":"42"},"meta":{"campaign":"spring"}}
        """.trimIndent().toByteArray()

        val payload = extractor.extract(data)!!

        assertEquals("fullscreen", payload.actionName)
        assertEquals("42", payload.params["id"])
        @Suppress("UNCHECKED_CAST")
        assertEquals("spring", (payload.meta as Map<String, Any?>)["campaign"])
    }

    @Test
    fun `returns null when event name is blank`() {
        val data = """{"event":"","params":{}}""".toByteArray()
        assertNull(extractor.extract(data))
    }

    @Test
    fun `returns null when event key is missing`() {
        val data = """{"params":{"a":"b"}}""".toByteArray()
        assertNull(extractor.extract(data))
    }

    @Test
    fun `returns null for malformed json`() {
        assertNull(extractor.extract("not json".toByteArray()))
    }

    @Test
    fun `missing params yields empty map`() {
        val data = """{"event":"view"}""".toByteArray()
        val payload = extractor.extract(data)!!
        assertEquals(emptyMap<String, String>(), payload.params)
    }

    @Test
    fun `json null meta normalizes to null`() {
        val data = """{"event":"view","meta":null}""".toByteArray()
        val payload = extractor.extract(data)!!
        assertNull(payload.meta)
    }
}
