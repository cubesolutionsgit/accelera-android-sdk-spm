package ai.accelera.library.banners.presentation.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateMachineTest {
    private val noOpLogger = StoriesStateLogger { }

    @Test
    fun `open transitions idle to preparing`() {
        val machine = PlaybackStateMachine(noOpLogger)
        val ok = machine.onEvent(PlaybackEvent.Open("entry-1"))
        assertTrue(ok)
        assertEquals(PlaybackState.PreparingEntry, machine.state)
    }

    @Test
    fun `tap next ignored in idle`() {
        val machine = PlaybackStateMachine(noOpLogger)
        val ok = machine.onEvent(PlaybackEvent.TapNext)
        assertFalse(ok)
        assertEquals(PlaybackState.Idle, machine.state)
    }

    @Test
    fun `destroyed is terminal state`() {
        val machine = PlaybackStateMachine(noOpLogger)
        machine.onEvent(PlaybackEvent.Open("entry-1"))
        machine.onEvent(PlaybackEvent.Destroyed)
        val ok = machine.onEvent(PlaybackEvent.ActivityResume)
        assertFalse(ok)
        assertEquals(PlaybackState.Destroyed, machine.state)
    }
}
