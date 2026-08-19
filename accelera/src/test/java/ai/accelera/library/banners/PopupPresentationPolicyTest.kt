package ai.accelera.library.banners

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupPresentationPolicyTest {
    @Test
    fun `new popup request supersedes previous request`() {
        val arbiter = PopupRequestArbiter()
        val pageA = arbiter.beginRequest()
        val pageB = arbiter.beginRequest()

        assertFalse(arbiter.isLatest(pageA))
        assertTrue(arbiter.isLatest(pageB))
    }

    @Test
    fun `page B wins when page A response arrives first`() {
        val arbiter = PopupRequestArbiter()
        val pageA = arbiter.beginRequest()
        val pageB = arbiter.beginRequest()

        val pageADecision = PopupResultPolicy.decide(
            result = "A".toByteArray(),
            error = null,
            canPresent = arbiter.isLatest(pageA)
        )
        val pageBDecision = PopupResultPolicy.decide(
            result = "B".toByteArray(),
            error = null,
            canPresent = arbiter.isLatest(pageB)
        )

        assertEquals(PopupResultDecision.ScreenInactive, pageADecision)
        assertTrue(pageBDecision is PopupResultDecision.Present)
    }

    @Test
    fun `page B wins when page B response arrives first`() {
        val arbiter = PopupRequestArbiter()
        val pageA = arbiter.beginRequest()
        val pageB = arbiter.beginRequest()

        val pageBDecision = PopupResultPolicy.decide(
            result = "B".toByteArray(),
            error = null,
            canPresent = arbiter.isLatest(pageB)
        )
        val latePageADecision = PopupResultPolicy.decide(
            result = "A".toByteArray(),
            error = null,
            canPresent = arbiter.isLatest(pageA)
        )

        assertTrue(pageBDecision is PopupResultDecision.Present)
        assertEquals(PopupResultDecision.ScreenInactive, latePageADecision)
    }

    @Test
    fun `latest request is still rejected after its page leaves resumed state`() {
        val arbiter = PopupRequestArbiter()
        val pageA = arbiter.beginRequest()

        val canPresent = arbiter.isLatest(pageA) && activePolicy(
            lifecycleState = Lifecycle.State.CREATED
        )

        assertFalse(canPresent)
    }

    @Test
    fun `load error takes precedence over screen state`() {
        val error = IllegalStateException("network failed")

        val decision = PopupResultPolicy.decide(
            result = null,
            error = error,
            canPresent = false
        )

        assertEquals(PopupResultDecision.LoadFailed(error), decision)
    }

    @Test
    fun `empty response is rejected before presentation`() {
        assertEquals(
            PopupResultDecision.Empty,
            PopupResultPolicy.decide(result = null, error = null, canPresent = true)
        )
    }

    @Test
    fun `loaded response is skipped when screen became inactive`() {
        assertEquals(
            PopupResultDecision.ScreenInactive,
            PopupResultPolicy.decide(
                result = "payload".toByteArray(),
                error = null,
                canPresent = false
            )
        )
    }

    @Test
    fun `loaded response is forwarded when screen remains active`() {
        val payload = "payload".toByteArray()

        val decision = PopupResultPolicy.decide(payload, error = null, canPresent = true)

        assertTrue(decision is PopupResultDecision.Present)
        assertTrue(payload.contentEquals((decision as PopupResultDecision.Present).data))
    }

    @Test
    fun `active resumed screen can present`() {
        assertTrue(
            PopupPresentationPolicy.canPresent(
                isSameActivity = true,
                isActivityAlive = true,
                hasWindowFocus = true,
                lifecycleState = Lifecycle.State.RESUMED
            )
        )
    }

    @Test
    fun `changed activity cannot present`() {
        assertFalse(activePolicy(isSameActivity = false))
    }

    @Test
    fun `finishing or destroyed activity cannot start or present`() {
        assertFalse(PopupPresentationPolicy.canStartLoading(isActivityAlive = false))
        assertFalse(activePolicy(isActivityAlive = false))
    }

    @Test
    fun `screen without window focus cannot present`() {
        assertFalse(activePolicy(hasWindowFocus = false))
    }

    @Test
    fun `non-resumed lifecycle states cannot present`() {
        listOf(
            Lifecycle.State.INITIALIZED,
            Lifecycle.State.CREATED,
            Lifecycle.State.STARTED,
            Lifecycle.State.DESTROYED
        ).forEach { state ->
            assertFalse("State $state must be rejected", activePolicy(lifecycleState = state))
        }
    }

    @Test
    fun `legacy host without lifecycle owner uses activity checks`() {
        assertTrue(activePolicy(lifecycleState = null))
    }

    @Test
    fun `completion gate accepts only first callback`() {
        val gate = PopupCompletionGate()

        assertTrue(gate.tryComplete())
        assertFalse(gate.tryComplete())
        assertFalse(gate.tryComplete())
    }

    @Test
    fun `successful launch keeps registered tokens`() {
        val removed = mutableListOf<String?>()
        var launchedWith: Pair<String, String?>? = null

        val result = PopupLaunchTransaction.launch(
            registerPayload = { "payload" },
            registerScope = { "scope" },
            startActivity = { payload, scope -> launchedWith = payload to scope },
            removePayload = removed::add,
            removeScope = removed::add
        )

        assertTrue(result.isSuccess)
        assertEquals("payload" to "scope", launchedWith)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `failed launch removes payload and scope tokens`() {
        val removedPayloads = mutableListOf<String?>()
        val removedScopes = mutableListOf<String?>()

        val result = PopupLaunchTransaction.launch(
            registerPayload = { "payload" },
            registerScope = { "scope" },
            startActivity = { _, _ -> error("launch rejected") },
            removePayload = removedPayloads::add,
            removeScope = removedScopes::add
        )

        assertTrue(result.isFailure)
        assertEquals(listOf("payload"), removedPayloads)
        assertEquals(listOf("scope"), removedScopes)
    }

    @Test
    fun `scope registration failure still removes payload`() {
        val removedPayloads = mutableListOf<String?>()

        val result = PopupLaunchTransaction.launch(
            registerPayload = { "payload" },
            registerScope = { error("scope registry unavailable") },
            startActivity = { _, _ -> error("must not launch") },
            removePayload = removedPayloads::add,
            removeScope = {}
        )

        assertTrue(result.isFailure)
        assertEquals(listOf("payload"), removedPayloads)
    }

    @Test
    fun `cleanup failure does not replace launch failure`() {
        val result = PopupLaunchTransaction.launch(
            registerPayload = { "payload" },
            registerScope = null,
            startActivity = { _, _ -> error("launch rejected") },
            removePayload = { error("cleanup rejected") },
            removeScope = { error("cleanup rejected") }
        )

        assertTrue(result.isFailure)
        assertEquals("launch rejected", result.exceptionOrNull()?.message)
    }

    private fun activePolicy(
        isSameActivity: Boolean = true,
        isActivityAlive: Boolean = true,
        hasWindowFocus: Boolean = true,
        lifecycleState: Lifecycle.State? = Lifecycle.State.RESUMED
    ): Boolean = PopupPresentationPolicy.canPresent(
        isSameActivity = isSameActivity,
        isActivityAlive = isActivityAlive,
        hasWindowFocus = hasWindowFocus,
        lifecycleState = lifecycleState
    )
}
