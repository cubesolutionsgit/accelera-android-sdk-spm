package ai.accelera.library.banners

import android.view.ViewGroup
import java.util.WeakHashMap

internal object AcceleraAttachedContentRegistry {
    private val contexts = WeakHashMap<ViewGroup, AcceleraAttachedContentContext>()

    @Synchronized
    fun register(container: ViewGroup, context: AcceleraAttachedContentContext) {
        contexts[container] = context
    }

    @Synchronized
    fun get(container: ViewGroup): AcceleraAttachedContentContext? = contexts[container]

    @Synchronized
    fun unregister(container: ViewGroup) {
        contexts.remove(container)
    }
}
