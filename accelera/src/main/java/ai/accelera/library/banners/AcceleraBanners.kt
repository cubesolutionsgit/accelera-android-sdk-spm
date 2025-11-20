package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.parentActivity
import ai.accelera.library.utils.toJsonBytes
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Extension functions for Accelera banners module (similar to Accelera+Banners in iOS).
 */
object AcceleraBanners {

    /**
     * Loads and attaches dynamic content into the given container.
     *
     * This method:
     * - Clears previous views in container
     * - Loads data using `loadBanner` from `AcceleraAPI`
     * - Parses the DivKit JSON
     * - Attaches and renders `Div2View` inside the container
     * - Optionally adds a close button if `jsonData.closable == true`
     *
     * @param container The ViewGroup that will host the banner.
     * @param data Optional input JSON to be sent to the backend.
     */
    fun attachContentPlaceholder(
        container: ViewGroup,
        data: ByteArray? = null
    ) {
        // Clear previous views
        container.removeAllViews()

        val activity = container.parentActivity
        if (activity == null) {
            Accelera.shared.error("No activity context available to present from.")
            return
        }

        val paramsString = data?.let { String(it, Charsets.UTF_8) } ?: "<invalid>"
        Accelera.shared.log("Loading content with params: $paramsString")

        val dataWithUserInfo = Accelera.shared.addUserInfo(to = data)
        Accelera.shared.getApi().loadBanner(dataWithUserInfo) { result, error ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (error != null) {
                    Accelera.shared.error("Failed to load content: ${error}")
                    return@post
                }

                val jsonData = result ?: run {
                    Accelera.shared.error("Empty JSON data from API")
                    return@post
                }

                Accelera.shared.log("Content loaded")

                try {
                    // Activity may implement LifecycleOwner (AppCompatActivity does)
                    val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
                    val divView = DivKitSetup.makeView(activity, jsonData, lifecycleOwner)
                    
                    container.addView(divView)
                    
                    // Set constraints similar to iOS NSLayoutConstraint
                    divView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    container.requestLayout()

                    // Set source asynchronously (similar to iOS Task { await divView.setSource(source) })
                    val divData = DivKitSetup.parseDivData(jsonData)
                    if (divData != null) {
                        val tag = com.yandex.div.DivDataTag("accelera_${System.currentTimeMillis()}")
                        divView.setData(divData, tag)
                    }

                    // Log view event (similar to iOS: self.logEvent(event: ["event": "view", "meta": jsonData.meta].asData))
                    val metaValue = jsonData.meta
                    val eventPayload = mapOf(
                        "event" to "view",
                        "meta" to (metaValue ?: emptyMap<String, Any?>())
                    )
                    Accelera.shared.logEvent(eventPayload.toJsonBytes())

                    // Add close button if closable (similar to iOS: if jsonData.closable == true)
                    if (jsonData.closable == true) {
                        val closeButton = CloseButton(activity).apply {
                            setOnClickListener {
                                // Remove divView from container (similar to iOS: divView.removeFromSuperview())
                                container.removeView(divView)
                            }
                        }
                        
                        divView.addView(closeButton)
                        divView.bringChildToFront(closeButton)
                        
                        // Set constraints (similar to iOS NSLayoutConstraint)
                        val params = ViewGroup.MarginLayoutParams(
                            (24 * activity.resources.displayMetrics.density).toInt(),
                            (24 * activity.resources.displayMetrics.density).toInt()
                        ).apply {
                            topMargin = (8 * activity.resources.displayMetrics.density).toInt()
                            marginEnd = (8 * activity.resources.displayMetrics.density).toInt()
                        }
                        closeButton.layoutParams = params
                    }
                } catch (e: Exception) {
                    Accelera.shared.error("Failed to create DivView: ${e.message}")
                }
            }
        }
    }
}
