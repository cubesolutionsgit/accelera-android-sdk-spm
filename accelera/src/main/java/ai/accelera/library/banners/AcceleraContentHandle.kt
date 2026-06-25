package ai.accelera.library.banners

import java.lang.ref.WeakReference

/**
 * Handle for content attached with [AcceleraBanners.attachContentPlaceholder].
 */
class AcceleraContentHandle internal constructor(
    context: AcceleraAttachedContentContext
) {
    private val contextRef = WeakReference(context)

    /**
     * Reloads content using the original request data plus `{"refresh": true}`.
     */
    fun refresh() {
        contextRef.get()?.load(isInitialLoad = false)
    }

    /**
     * Removes attached content and logs the close event.
     */
    fun detach() {
        contextRef.get()?.detach()
    }
}
