package ai.accelera.library.core.config

import ai.accelera.library.AcceleraConfig
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryConfigStoreTest {
    @Test
    fun `updateUserInfo returns null when config is missing`() {
        val store = InMemoryConfigStore()
        assertNull(store.updateUserInfo("""{"id":"1"}"""))
    }

    @Test
    fun `updateUserInfo merges into existing config`() {
        val store = InMemoryConfigStore()
        store.setConfig(AcceleraConfig(url = "https://example.com", userInfo = """{"name":"john"}"""))

        val updated = store.updateUserInfo("""{"age":20}""")
        val merged = updated?.userInfo.orEmpty()
        assertEquals(true, merged.isNotBlank())
        assertEquals(true, merged.contains("\"age\":20"))
    }
}
