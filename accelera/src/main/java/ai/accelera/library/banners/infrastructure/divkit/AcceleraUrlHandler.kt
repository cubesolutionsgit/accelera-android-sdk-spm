package ai.accelera.library.banners.infrastructure.divkit

import ai.accelera.library.Accelera
import ai.accelera.library.banners.presentation.ui.FullscreenActivity
import ai.accelera.library.utils.toJsonBytes
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.yandex.div.core.DivActionHandler
import com.yandex.div.core.DivViewFacade
import com.yandex.div.json.expressions.ExpressionResolver
import com.yandex.div2.DivAction
import org.json.JSONObject

/** Recursively unwraps [ContextWrapper] layers to find the underlying [Activity], if any. */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * URL handler for DivKit actions.
 * Handles div-action:// URLs for fullscreen, link, and close actions.
 */
internal class AcceleraUrlHandler(
    private val context: Context,
    private val jsonData: ByteArray
) : DivActionHandler() {

    override fun handleAction(
        action: DivAction,
        view: DivViewFacade,
        resolver: ExpressionResolver
    ): Boolean {
        super.handleAction(action, view, resolver)
        val url = action.url ?: return false

        Accelera.Companion.shared.log("Divkit action: $url")

        val uri = action.url?.evaluate(view.expressionResolver) ?: return false
        if (uri.scheme != "div-action") return false

        val actionType = uri.host ?: ""
        val actionParams = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach { key ->
            uri.getQueryParameter(key)?.let { value ->
                actionParams[key] = value
            }
        }

        val meta = extractMeta(jsonData)
        val payload = mapOf(
            "event" to actionType,
            "params" to actionParams,
            "meta" to meta
        )

        Accelera.Companion.shared.logEvent(payload.toJsonBytes())

        when (actionType) {
            "fullscreen" -> {
                val id = uri.getQueryParameter("id") ?: return false

                val activity = context.findActivity() ?: run {
                    Accelera.Companion.shared.error("No Activity context available to start FullscreenActivity")
                    return false
                }

                val intent = Intent(activity, FullscreenActivity::class.java).apply {
                    putExtra("jsonData", jsonData)
                    putExtra("entryId", id)
                }
                activity.startActivity(intent)
                return true
            }

            "link" -> {
                val urlParam = uri.getQueryParameter("url")
                if (urlParam != null) {
                    try {
                        val finalUrl = Uri.parse(urlParam)
                        // Fire the deep-link first so MainActivity navigates while the close
                        // animation plays — the user lands on the correct destination instantly.
                        Accelera.Companion.shared.handleUrl(finalUrl)

                        // If we're inside FullscreenActivity, ask the delegate whether to close it.
                        // Default is true so standard deep-link navigation is always visible.
                        val activity = context.findActivity()
                        if (activity is FullscreenActivity) {
                            val shouldDismiss = Accelera.Companion.shared.getDelegate()
                                ?.shouldDismissStoriesOnLink(finalUrl) ?: true
                            if (shouldDismiss) activity.requestClose()
                        }
                        return true
                    } catch (e: Exception) {
                        Accelera.Companion.shared.error("Could not construct URL from: $urlParam")
                    }
                } else {
                    Accelera.Companion.shared.error("No 'url' parameter found in link action")
                }
            }

            "close" -> {
                // Close current activity if it's an Activity context
                if (context is Activity) {
                    context.finish()
                    return true
                }
            }

            else -> Unit
        }
        return false
    }

    private fun extractMeta(jsonData: ByteArray): Map<String, Any?> {
        return runCatching {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val card = root.optJSONObject("card")
            val meta = card?.optJSONObject("meta") ?: return emptyMap()

            buildMap {
                val keys = meta.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, meta.get(key))
                }
            }
        }.getOrElse { emptyMap() }
    }
}