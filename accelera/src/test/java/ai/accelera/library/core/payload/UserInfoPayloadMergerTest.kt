package ai.accelera.library.core.payload

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserInfoPayloadMergerTest {

    private val merger = JsonUserInfoPayloadMerger()

    private fun ByteArray?.json() = JSONObject(String(this!!, Charsets.UTF_8))

    @Test
    fun `keeps payload fields and nests user info`() {
        val payload = """{"event":"view"}""".toByteArray()

        val result = merger.mergeUserInfo(payload, """{"id":7}""").json()

        assertEquals("view", result.getString("event"))
        assertEquals(7, result.getJSONObject("userInfo").getInt("id"))
    }

    @Test
    fun `works with null payload`() {
        val result = merger.mergeUserInfo(null, """{"id":1}""").json()
        assertEquals(1, result.getJSONObject("userInfo").getInt("id"))
    }

    @Test
    fun `blank user info is ignored`() {
        val result = merger.mergeUserInfo("""{"a":1}""".toByteArray(), "").json()
        assertEquals(1, result.getInt("a"))
        assertFalse(result.has("userInfo"))
    }

    @Test
    fun `malformed user info is skipped without throwing`() {
        val result = merger.mergeUserInfo("""{"a":1}""".toByteArray(), "not json").json()
        assertEquals(1, result.getInt("a"))
        assertFalse(result.has("userInfo"))
    }
}
