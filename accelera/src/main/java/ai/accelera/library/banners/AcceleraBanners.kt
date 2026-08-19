package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.usecase.DefaultLoadBannerContentUseCase
import ai.accelera.library.banners.infrastructure.activity.AcceleraActivityTracker
import ai.accelera.library.banners.infrastructure.cache.AcceleraPayloadRegistry
import ai.accelera.library.banners.infrastructure.divkit.AcceleraDivVariableScope
import ai.accelera.library.banners.infrastructure.divkit.AcceleraScopeRegistry
import ai.accelera.library.banners.presentation.ui.PopupActivity
import ai.accelera.library.utils.parentActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/**
 * Extension functions for Accelera banners module (similar to Accelera+Banners in iOS).
 */
object AcceleraBanners {
    private val popupRequestArbiter = PopupRequestArbiter()

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
            safeError("No activity context available to present popup.")
            return
        }
        showPopup(activity, activity as? LifecycleOwner, data, variableScope = null)
    }

    /**
     * Shows a popup scoped to the resolved Activity.
     *
     * Compose Navigation destinations share one Activity, so they should use
     * `rememberAcceleraPopupController()` to bind the request to their own lifecycle.
     */
    fun showPopup(context: Context, data: ByteArray? = null) {
        val activity = runCatching { context.parentActivity ?: AcceleraActivityTracker.currentActivity() }
            .getOrNull()
        if (activity == null) {
            safeError("No activity context available to present popup.")
            return
        }
        showPopup(activity, activity as? LifecycleOwner, data, variableScope = null)
    }

    /** Shows a popup only while [lifecycleOwner] remains the active screen. */
    fun showPopup(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        data: ByteArray? = null
    ) {
        showPopup(activity, lifecycleOwner, data, variableScope = null)
    }

    private fun showPopup(
        activity: Activity,
        lifecycleOwner: LifecycleOwner?,
        data: ByteArray?,
        variableScope: AcceleraDivVariableScope?
    ) {
        runCatching { AcceleraActivityTracker.register(activity) }
            .onFailure { safeError("Failed to track popup host activity: ${it.message}") }

        val activityAlive = !activity.isFinishing && !activity.isDestroyed
        if (!PopupPresentationPolicy.canStartLoading(activityAlive)) {
            safeLog(SKIPPED_MESSAGE)
            return
        }

        // Lifecycle callbacks are not replayed when the tracker is first registered.
        // Seed it only for a genuinely current/focused host or when no host is known.
        val trackedActivity = AcceleraActivityTracker.currentActivity()
        if (trackedActivity == null || activity.hasWindowFocus()) {
            runCatching { AcceleraActivityTracker.note(activity) }
                .onFailure { safeError("Failed to track popup host activity: ${it.message}") }
        } else if (trackedActivity !== activity) {
            safeLog(SKIPPED_MESSAGE)
            return
        }

        val activityRef = WeakReference(activity)
        val lifecycleOwnerRef = lifecycleOwner?.let(::WeakReference)
        val completionGate = PopupCompletionGate()
        val requestId = popupRequestArbiter.beginRequest()

        val paramsString = data?.let { String(it, Charsets.UTF_8) } ?: "<invalid>"
        safeLog("Loading popup content with params: $paramsString")

        runCatching {
            DefaultLoadBannerContentUseCase(logViewEvent = false).load(data) { result, error ->
                if (!completionGate.tryComplete()) return@load
                val posted = runCatching {
                    Handler(Looper.getMainLooper()).post {
                        runCatching {
                            handlePopupResult(
                                activityRef = activityRef,
                                lifecycleOwnerRef = lifecycleOwnerRef,
                                result = result,
                                error = error,
                                variableScope = variableScope,
                                requestId = requestId
                            )
                        }.onFailure { safeError("Failed to present popup: ${it.message}") }
                    }
                }.getOrElse {
                    safeError("Failed to dispatch popup presentation: ${it.message}")
                    false
                }
                if (!posted) safeError("Failed to dispatch popup presentation to the main thread")
            }
        }.onFailure { safeError("Failed to load popup content: ${it.message}") }
    }

    private fun handlePopupResult(
        activityRef: WeakReference<Activity>,
        lifecycleOwnerRef: WeakReference<LifecycleOwner>?,
        result: ByteArray?,
        error: Any?,
        variableScope: AcceleraDivVariableScope?,
        requestId: Long
    ) {
        val activity = activityRef.get()
        val owner = lifecycleOwnerRef?.get()
        val lifecycleState = owner?.lifecycle?.currentState
        val canPresent = popupRequestArbiter.isLatest(requestId) &&
            activity != null && PopupPresentationPolicy.canPresent(
            isSameActivity = AcceleraActivityTracker.currentActivity() === activity,
            isActivityAlive = !activity.isFinishing && !activity.isDestroyed,
            hasWindowFocus = activity.hasWindowFocus(),
            lifecycleState = lifecycleState
        ) && (lifecycleOwnerRef == null || owner != null)

        val jsonData = when (val decision = PopupResultPolicy.decide(result, error, canPresent)) {
            is PopupResultDecision.LoadFailed -> {
                safeError("Failed to load popup content: ${decision.error}")
                return
            }
            PopupResultDecision.Empty -> {
                safeError("Empty popup JSON data from API")
                return
            }
            PopupResultDecision.ScreenInactive -> {
                safeLog(SKIPPED_MESSAGE)
                return
            }
            is PopupResultDecision.Present -> decision.data
        }
        val presentingActivity = activity ?: run {
            safeLog(SKIPPED_MESSAGE)
            return
        }

        val launchResult = PopupLaunchTransaction.launch(
            registerPayload = { AcceleraPayloadRegistry.register(jsonData) },
            registerScope = variableScope?.let { scope ->
                { AcceleraScopeRegistry.register(scope) }
            },
            startActivity = { payloadToken, scopeToken ->
                val intent = Intent(presentingActivity, PopupActivity::class.java).apply {
                    putExtra(PopupActivity.EXTRA_PAYLOAD_TOKEN, payloadToken)
                    scopeToken?.let { putExtra(PopupActivity.EXTRA_SCOPE_TOKEN, it) }
                }
                presentingActivity.startActivity(intent)
            },
            removePayload = AcceleraPayloadRegistry::remove,
            removeScope = AcceleraScopeRegistry::remove
        )
        if (launchResult.isFailure) {
            safeError("Failed to present popup: ${launchResult.exceptionOrNull()?.message}")
            return
        }

        runCatching {
            presentingActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.onFailure { safeError("Failed to animate popup presentation: ${it.message}") }
    }

    private fun safeLog(message: String) {
        runCatching { Accelera.shared.log(message) }
    }

    private fun safeError(message: String) {
        runCatching { Accelera.shared.error(message) }
    }

    private const val SKIPPED_MESSAGE =
        "Popup skipped because presenting screen is no longer active"
}
