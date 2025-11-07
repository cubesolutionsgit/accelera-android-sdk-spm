package ai.accelera.library.banners

import android.content.Context
import android.net.Uri
import ai.accelera.library.Accelera
import ai.accelera.library.utils.toJsonBytes
import android.app.Activity
import android.content.Intent
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
    private val jsonData: ByteArray
) : DivActionHandler() {

    override fun handleAction(
        action: DivAction,
        view: DivViewFacade,
        resolver: ExpressionResolver
    ): Boolean {
        super.handleAction(action, view, resolver)
        val url = action.url ?: return false

        Accelera.shared.log("Divkit action: $url")

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

        Accelera.shared.logEvent(payload.toJsonBytes())

        when (actionType) {
            "fullscreen" -> {
                val id = uri.getQueryParameter("id") ?: return false
                val intent = Intent(context, FullscreenActivity::class.java).apply {
                    putExtra("jsonData", jsonData)
                    putExtra("entryId", id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            }

            "link" -> {
                val urlParam = uri.getQueryParameter("url")
                if (urlParam != null) {
                    try {
                        val finalUrl = Uri.parse(urlParam)
                        Accelera.shared.handleUrl(finalUrl)
                        return true
                    } catch (e: Exception) {
                        Accelera.shared.error("Could not construct URL from: $urlParam")
                    }
                } else {
                    Accelera.shared.error("No 'url' parameter found in link action")
                }
            }

            "close" -> {
                // Close current activity if it's an Activity context
                if (context is Activity) {
                    context.finish()
                    return true
                }
            }

            else -> {
                Accelera.shared.error("Unknown div-action type: $actionType")
            }
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
