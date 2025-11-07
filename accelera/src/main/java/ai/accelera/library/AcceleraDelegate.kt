package ai.accelera.library

import ai.accelera.library.api.AcceleraAPIProtocol
import android.net.Uri

/**
 * Delegate protocol for handling events, actions, logging, and custom API override from Accelera.
 */
interface AcceleraDelegate {
    /**
     * Called when the library wants to log a message.
     * @param message The log string.
     */
    fun log(message: String)

    /**
     * Called when the library wants to report an error.
     * @param error The error string.
     */
    fun error(error: String)

    /**
     * Optional custom API implementation override.
     * Return your own conforming instance if you want to bypass the default [AcceleraAPI].
     */
    val customAPI: AcceleraAPIProtocol?

    /**
     * Handles incoming deep links or internal URLs.
     * @param url The URL to handle.
     */
    fun handleUrl(url: Uri)

    /**
     * Handles custom action strings triggered by banners or other modules.
     * @param action Action identifier string.
     */
    fun action(action: String)
}

/**
 * Default implementation of [AcceleraDelegate] with no-op methods.
 * You can extend this class and override only the methods you need.
 */
open class DefaultAcceleraDelegate : AcceleraDelegate {
    override fun log(message: String) {
        android.util.Log.d("Accelera", message)
    }

    override fun error(error: String) {
        android.util.Log.e("Accelera", error)
    }

    override val customAPI: AcceleraAPIProtocol? = null

    override fun handleUrl(url: Uri) {
        // Default implementation - can be overridden
        android.util.Log.d("Accelera", "Handle URL: $url")
    }

    override fun action(action: String) {
        android.util.Log.d("Accelera", "Banner action: $action")
    }
}

