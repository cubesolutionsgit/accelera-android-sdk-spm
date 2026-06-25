package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.usecase.DefaultLoadBannerContentUseCase
import ai.accelera.library.banners.infrastructure.activity.AcceleraActivityTracker
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.banners.presentation.ui.CloseButton
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.mergeJSON
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.parentActivity
import ai.accelera.library.utils.toJsonBytes
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.yandex.div.DivDataTag
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class AcceleraAttachedContentContext(
    container: ViewGroup,
    private val data: ByteArray?
) {
    private val containerRef = WeakReference(container)
    private val loadBannerContentUseCase = DefaultLoadBannerContentUseCase()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var divView: Div2View? = null
    private var jsonData: ByteArray? = null
    private var isRefreshing = false

    fun load(isInitialLoad: Boolean = false) {
        val container = containerRef.get() ?: return
        if (isRefreshing) return
        isRefreshing = true

        if (isInitialLoad) {
            container.removeAllViews()
        }

        val activity = container.parentActivity
        if (activity == null) {
            Accelera.shared.error("No activity context available to present from.")
            isRefreshing = false
            return
        }
        AcceleraActivityTracker.note(activity)

        val requestData = if (isInitialLoad) {
            data
        } else {
            mergeJSON(
                old = data?.let { String(it, Charsets.UTF_8) },
                new = """{"refresh":true}"""
            )?.toByteArray(Charsets.UTF_8)
        }

        val paramsString = requestData?.let { String(it, Charsets.UTF_8) } ?: "<invalid>"
        Accelera.shared.log("Loading content with params: $paramsString")

        loadBannerContentUseCase.load(requestData) { result, error ->
            mainHandler.post {
                try {
                    if (error != null) {
                        Accelera.shared.error("Failed to load content: $error")
                        return@post
                    }

                    val loadedData = result ?: run {
                        Accelera.shared.error("Empty JSON data from API")
                        return@post
                    }

                    Accelera.shared.log("Content loaded")
                    render(container, loadedData)
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    fun detach() {
        val meta = (jsonData?.meta as? JSONObject) ?: JSONObject()
        val payload = mapOf("event" to "close", "meta" to meta)
        Accelera.shared.logEvent(payload.toJsonBytes())
        remove()
    }

    fun remove() {
        val container = containerRef.get()
        divView?.let { view ->
            DivKitSetup.releaseVideoPlayers(view)
            container?.removeView(view)
        }
        divView = null
        jsonData = null
        container?.let { AcceleraAttachedContentRegistry.unregister(it) }
    }

    private fun render(container: ViewGroup, loadedData: ByteArray) {
        val activity = container.parentActivity ?: run {
            Accelera.shared.error("No activity context available to render content.")
            return
        }

        try {
            val oldDivView = divView
            val lifecycleOwner = activity as? LifecycleOwner
            val newDivView = DivKitSetup.makeView(
                context = activity,
                jsonData = loadedData,
                lifecycleOwner = lifecycleOwner,
                originContext = this
            )

            container.addView(
                newDivView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            container.requestLayout()

            val divData = DivKitSetup.parseDivData(loadedData)
            if (divData != null) {
                val tag = DivDataTag("accelera_${System.currentTimeMillis()}")
                newDivView.setData(divData, tag)
            }

            if (loadedData.closable == true) {
                addCloseButton(newDivView)
            }

            oldDivView?.let {
                DivKitSetup.releaseVideoPlayers(it)
                container.removeView(it)
            }
            divView = newDivView
            jsonData = loadedData
        } catch (e: Exception) {
            Accelera.shared.error("Failed to create DivView: ${e.message}")
        }
    }

    private fun addCloseButton(divView: Div2View) {
        val activity = divView.context.parentActivity ?: return
        val density = activity.resources.displayMetrics.density
        val closeButton = CloseButton(activity).apply {
            setOnClickListener { detach() }
        }

        divView.addView(closeButton)
        divView.bringChildToFront(closeButton)

        val params = ViewGroup.MarginLayoutParams(
            (24 * density).toInt(),
            (24 * density).toInt()
        ).apply {
            topMargin = (8 * density).toInt()
            marginEnd = (8 * density).toInt()
        }
        closeButton.layoutParams = params
    }
}
