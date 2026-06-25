package ai.accelera.library.utils

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ByteArrayExtensionsTest {

    @Test
    fun `reads closable duration and meta from a plain object`() {
        val bytes = """
            {"closable":true,"duration":3000,"meta":{"campaign":"x"}}
        """.trimIndent().toByteArray()

        assertEquals(true, bytes.closable)
        assertEquals(3000, bytes.duration)
        assertEquals("x", (bytes.meta as JSONObject).getString("campaign"))
    }

    @Test
    fun `reads values from the first card of a cards response`() {
        val bytes = """
            {"cards":[{"closable":false,"duration":1000,"meta":{"k":"v"}}]}
        """.trimIndent().toByteArray()

        assertEquals(false, bytes.closable)
        assertEquals(1000, bytes.duration)
        assertEquals("v", (bytes.meta as JSONObject).getString("k"))
    }

    @Test
    fun `returns null for malformed json`() {
        val bytes = "definitely not json".toByteArray()

        assertNull(bytes.closable)
        assertNull(bytes.duration)
        assertNull(bytes.meta)
    }

    @Test
    fun `returns null when keys are absent`() {
        val bytes = """{"something":"else"}""".toByteArray()

        assertNull(bytes.closable)
        assertNull(bytes.duration)
        assertNull(bytes.meta)
    }
}
