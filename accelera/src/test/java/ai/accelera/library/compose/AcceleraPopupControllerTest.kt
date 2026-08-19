package ai.accelera.library.compose

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Test

class AcceleraPopupControllerTest {
    @Test
    fun `controller without activity safely ignores show`() {
        val owner = object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = LifecycleRegistryStub
        }
        val controller = AcceleraPopupController(activity = null, lifecycleOwner = owner)

        controller.show("payload".toByteArray())
    }

    private object LifecycleRegistryStub : Lifecycle() {
        override val currentState: State = State.RESUMED
        override fun addObserver(observer: androidx.lifecycle.LifecycleObserver) = Unit
        override fun removeObserver(observer: androidx.lifecycle.LifecycleObserver) = Unit
    }
}
