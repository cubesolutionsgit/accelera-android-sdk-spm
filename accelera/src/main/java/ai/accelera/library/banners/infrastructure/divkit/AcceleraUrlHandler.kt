package ai.accelera.library.banners.infrastructure.divkit

import ai.accelera.library.Accelera
import ai.accelera.library.banners.AcceleraAttachedContentContext
import ai.accelera.library.banners.presentation.ui.FullscreenActivity
import ai.accelera.library.core.constants.AcceleraActionQuery
import ai.accelera.library.core.constants.AcceleraActionTypes
import ai.accelera.library.core.constants.AcceleraJsonKeys
import ai.accelera.library.utils.parentActivity
import ai.accelera.library.utils.toJsonBytes
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yandex.div.core.DivActionHandler
import com.yandex.div.core.DivViewFacade
import com.yandex.div.json.expressions.ExpressionResolver
import com.yandex.div2.DivAction
import org.json.JSONObject

/**
 * URL handler for DivKit actions.
 * Handles div-action:// URLs for fullscreen, link, and close actions.
 */
internal class AcceleraUrlHandler(
    private val context: Context,
    private val jsonData: ByteArray,
    private val originContext: AcceleraAttachedContentContext? = null
) : DivActionHandler() {

    override fun handleAction(
        action: DivAction,
        view: DivViewFacade,
        resolver: ExpressionResolver
    ): Boolean {
        // DivKit invokes this from its own dispatch; never let a malformed action
        // or downstream failure crash the host app.
        return runCatching { handleAcceleraAction(action, view, resolver) }
            .onFailure { Accelera.shared.error("Failed to handle DivKit action: ${it.message}") }
            .getOrDefault(false)
    }

    private fun handleAcceleraAction(
        action: DivAction,
        view: DivViewFacade,
        resolver: ExpressionResolver
    ): Boolean {
        super.handleAction(action, view, resolver)
        val url = action.url ?: return false

        Accelera.shared.log("Divkit action: $url")

        val uri = action.url?.evaluate(view.expressionResolver) ?: return false
        if (uri.scheme != AcceleraActionTypes.SCHEME) return false

        val actionType = uri.host ?: ""
        val actionParams = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach { key ->
            uri.getQueryParameter(key)?.let { value ->
                actionParams[key] = value
            }
        }

        val meta = extractMeta(jsonData)
        val payload = mapOf(
            AcceleraJsonKeys.EVENT to actionType,
            AcceleraJsonKeys.PARAMS to actionParams,
            AcceleraJsonKeys.META to meta
        )

        if (!uri.queryParameterNames.contains(AcceleraActionQuery.IGNORE)) {
            Accelera.shared.logEvent(payload.toJsonBytes())
        }

        when (actionType) {
            AcceleraActionTypes.FULLSCREEN -> {
                val id = uri.getQueryParameter(AcceleraActionQuery.ID) ?: return false

                val activity = context.parentActivity ?: run {
                    Accelera.shared.error("No Activity context available to start FullscreenActivity")
                    return false
                }

                val intent = Intent(activity, FullscreenActivity::class.java).apply {
                    putExtra(FullscreenActivity.EXTRA_JSON_DATA, jsonData)
                    putExtra(FullscreenActivity.EXTRA_ENTRY_ID, id)
                    originContext?.registerSharedVariableScope()?.let { token ->
                        putExtra(FullscreenActivity.EXTRA_SCOPE_TOKEN, token)
                    }
                }
                activity.startActivity(intent)
                return true
            }

            AcceleraActionTypes.LINK -> {
                val urlParam = uri.getQueryParameter(AcceleraActionQuery.URL)
                if (urlParam != null) {
                    try {
                        val finalUrl = Uri.parse(urlParam)
                        // Fire the deep-link first so MainActivity navigates while the close
                        // animation plays — the user lands on the correct destination instantly.
                        Accelera.shared.handleUrl(finalUrl)

                        // If we're inside FullscreenActivity, ask the delegate whether to close it.
                        // Default is true so standard deep-link navigation is always visible.
                        val activity = context.parentActivity
                        if (activity is FullscreenActivity) {
                            val shouldDismiss = Accelera.shared.getDelegate()
                                ?.shouldDismissStoriesOnLink(finalUrl) ?: true
                            if (shouldDismiss) activity.requestClose()
                        }
                        return true
                    } catch (e: Exception) {
                        Accelera.shared.error("Could not construct URL from: $urlParam")
                    }
                } else {
                    Accelera.shared.error("No 'url' parameter found in link action")
                }
            }

            AcceleraActionTypes.CLOSE -> {
                val activity = context.parentActivity
                if (activity is FullscreenActivity) {
                    activity.requestClose()
                    return true
                }
                if (originContext != null) {
                    originContext.remove()
                    return true
                }
                if (activity != null) {
                    activity.finish()
                    return true
                }
            }

            AcceleraActionTypes.REFRESH -> {
                originContext?.load(isInitialLoad = false)
                return originContext != null
            }

            else -> Unit
        }
        return false
    }

    private fun extractMeta(jsonData: ByteArray): Map<String, Any?> {
        return runCatching {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val card = root.optJSONObject(AcceleraJsonKeys.CARD)
            val meta = card?.optJSONObject(AcceleraJsonKeys.META) ?: return emptyMap()

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
