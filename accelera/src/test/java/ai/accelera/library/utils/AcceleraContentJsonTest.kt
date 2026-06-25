package ai.accelera.library.utils

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AcceleraContentJsonTest {
    @Test
    fun `normalizes api cards response to first div data card`() {
        val root = JSONObject(
            """
            {
              "cards": [
                {
                  "log_id": "fallback",
                  "states": [
                    {
                      "state_id": 0,
                      "div": {
                        "type": "text",
                        "text": "No content available"
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val divData = root.normalizedDivDataObjectOrNull()

        assertNotNull(divData)
        assertEquals("fallback", divData?.getString("log_id"))
        assertEquals(1, divData?.getJSONArray("states")?.length())
    }

    @Test
    fun `empty api cards response is treated as no content`() {
        val root = JSONObject("""{"cards":[]}""")

        assertNull(root.normalizedDivDataObjectOrNull())
    }

    @Test
    fun `direct div is wrapped into div data card`() {
        val root = JSONObject("""{"type":"text","text":"Hello"}""")

        val divData = root.normalizedDivDataObjectOrNull()

        assertEquals("accelera_generated", divData?.getString("log_id"))
        assertEquals("text", divData
            ?.getJSONArray("states")
            ?.getJSONObject(0)
            ?.getJSONObject("div")
            ?.getString("type"))
    }

    @Test
    fun `byte array metadata reads first card from api cards response`() {
        val payload = """
            {
              "cards": [
                {
                  "log_id": "card-1",
                  "meta": {"campaign": "fallback"},
                  "closable": true,
                  "states": []
                }
              ]
            }
            """.trimIndent().toByteArray()

        assertEquals(true, payload.closable)
        assertEquals("fallback", (payload.meta as JSONObject).getString("campaign"))
    }

    @Test
    fun `card with states but no log_id gets a generated id`() {
        val root = JSONObject(
            """
            {
              "card": {
                "states": [
                  {"state_id": 0, "div": {"type": "text"}}
                ]
              }
            }
            """.trimIndent()
        )

        val divData = root.normalizedDivDataObjectOrNull()

        assertEquals("accelera_generated", divData?.getString("log_id"))
    }

    @Test
    fun `card with states and log_id is returned unchanged`() {
        val root = JSONObject(
            """
            {"card": {"log_id": "keep-me", "states": [{"state_id": 0, "div": {"type": "text"}}]}}
            """.trimIndent()
        )

        assertEquals("keep-me", root.normalizedDivDataObjectOrNull()?.getString("log_id"))
    }

    @Test
    fun `object without states or recognizable div is treated as no content`() {
        val root = JSONObject("""{"card": {"unrelated": true}}""")

        assertNull(root.normalizedDivDataObjectOrNull())
    }
}
