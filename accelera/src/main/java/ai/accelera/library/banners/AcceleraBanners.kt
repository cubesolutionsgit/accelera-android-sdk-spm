package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.usecase.DefaultLoadBannerContentUseCase
import ai.accelera.library.banners.infrastructure.activity.AcceleraActivityTracker
import ai.accelera.library.banners.presentation.ui.PopupActivity
import ai.accelera.library.utils.parentActivity
import android.content.Context
import android.content.Intent
import android.view.ViewGroup

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
    ): AcceleraContentHandle {
        val context = AcceleraAttachedContentContext(container, data)
        AcceleraAttachedContentRegistry.register(container, context)
        context.load(isInitialLoad = true)
        return AcceleraContentHandle(context)
    }

    fun refreshContentPlaceholder(container: ViewGroup) {
        val context = AcceleraAttachedContentRegistry.get(container)
        if (context == null) {
            Accelera.shared.log("No content placeholder found to refresh")
            return
        }
        context.load(isInitialLoad = false)
    }

    fun detachContentPlaceholder(container: ViewGroup) {
        val context = AcceleraAttachedContentRegistry.get(container)
        if (context == null) {
            Accelera.shared.log("No content placeholder found to detach")
            return
        }
        context.detach()
    }

    fun showPopup(data: ByteArray? = null) {
        val activity = AcceleraActivityTracker.currentActivity()
        if (activity == null) {
            Accelera.shared.error("No activity context available to present popup.")
            return
        }
        showPopup(activity, data)
    }

    fun showPopup(context: Context, data: ByteArray? = null) {
        AcceleraActivityTracker.register(context)
        val activity = context.parentActivity ?: AcceleraActivityTracker.currentActivity()
        if (activity == null) {
            Accelera.shared.error("No activity context available to present popup.")
            return
        }
        AcceleraActivityTracker.note(activity)

        val paramsString = data?.let { String(it, Charsets.UTF_8) } ?: "<invalid>"
        Accelera.shared.log("Loading popup content with params: $paramsString")

        DefaultLoadBannerContentUseCase(logViewEvent = false).load(data) { result, error ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (error != null) {
                    Accelera.shared.error("Failed to load popup content: $error")
                    return@post
                }

                val jsonData = result ?: run {
                    Accelera.shared.error("Empty popup JSON data from API")
                    return@post
                }

                val intent = Intent(activity, PopupActivity::class.java).apply {
                    putExtra(PopupActivity.EXTRA_JSON_DATA, jsonData)
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }
}
