package ai.accelera.library.utils

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `toJsonBytes round trips a map`() {
        val bytes = mapOf("a" to 1, "b" to "x").toJsonBytes()
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        assertEquals(1, json.getInt("a"))
        assertEquals("x", json.getString("b"))
    }

    @Test
    fun `toJsonString round trips a map`() {
        val json = JSONObject(mapOf("k" to "v").toJsonString())
        assertEquals("v", json.getString("k"))
    }

    @Test
    fun `mergeJSON returns new when old is null`() {
        assertEquals("""{"a":1}""", mergeJSON(null, """{"a":1}"""))
    }

    @Test
    fun `mergeJSON returns old when new is null`() {
        assertEquals("""{"a":1}""", mergeJSON("""{"a":1}""", null))
    }

    @Test
    fun `mergeJSON lets new values win over old`() {
        val merged = JSONObject(mergeJSON("""{"a":1,"b":2}""", """{"b":9,"c":3}""")!!)
        assertEquals(1, merged.getInt("a"))
        assertEquals(9, merged.getInt("b"))
        assertEquals(3, merged.getInt("c"))
    }

    @Test
    fun `mergeJSON falls back to new on malformed old`() {
        assertEquals("""{"a":1}""", mergeJSON("not json", """{"a":1}"""))
    }

    @Test
    fun `refreshPayloadJson asks backend for refreshed content`() {
        val json = JSONObject(refreshPayloadJson())
        assertTrue(json.getBoolean("refresh"))
        assertEquals(1, json.length())
    }

    @Test
    fun `mergeJSON returns null when both inputs are null`() {
        assertNull(mergeJSON(null, null))
    }
}
