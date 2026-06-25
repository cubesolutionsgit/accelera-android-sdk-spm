package ai.accelera.library.banners.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryDataRepositoryTest {

    private fun repo(json: String) = StoryDataRepository(json.toByteArray())

    @Test
    fun `entry ids are returned sorted`() {
        val ids = repo("""{"fullscreens":{"c":{},"a":{},"b":{}}}""").loadEntryIds()
        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun `entry ids are empty when fullscreens missing`() {
        assertTrue(repo("""{}""").loadEntryIds().isEmpty())
    }

    @Test
    fun `loads cards for an entry`() {
        val cards = repo(
            """{"fullscreens":{"a":{"cards":[{"x":1},{"x":2}]}}}"""
        ).loadEntryCards("a")

        assertEquals(2, cards?.size)
        assertEquals(1, cards?.get(0)?.getInt("x"))
    }

    @Test
    fun `returns null cards for a missing entry`() {
        assertNull(repo("""{"fullscreens":{"a":{}}}""").loadEntryCards("missing"))
    }

    @Test
    fun `returns empty cards when entry has no cards array`() {
        assertEquals(emptyList<JSONObject>(), repo("""{"fullscreens":{"a":{}}}""").loadEntryCards("a"))
    }

    @Test
    fun `card duration defaults to five seconds when absent`() {
        assertEquals(5000L, repo("{}").getCardDuration(JSONObject("{}")))
    }

    @Test
    fun `card duration is read from top level and nested card`() {
        val r = repo("{}")
        assertEquals(1500L, r.getCardDuration(JSONObject("""{"duration":1500}""")))
        assertEquals(2000L, r.getCardDuration(JSONObject("""{"card":{"duration":2000}}""")))
    }

    @Test
    fun `hasDuration reflects presence of a positive duration`() {
        val r = repo("{}")
        assertTrue(r.hasDuration(JSONObject("""{"duration":10}""")))
        assertFalse(r.hasDuration(JSONObject("""{"duration":0}""")))
        assertFalse(r.hasDuration(JSONObject("{}")))
    }

    @Test
    fun `getCardMeta reads nested card meta or empty object`() {
        val r = repo("{}")
        assertEquals("v", r.getCardMeta(JSONObject("""{"card":{"meta":{"k":"v"}}}""")).getString("k"))
        assertEquals(0, r.getCardMeta(JSONObject("{}")).length())
    }
}
