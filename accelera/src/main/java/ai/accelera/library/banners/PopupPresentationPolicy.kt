package ai.accelera.library.banners

import androidx.lifecycle.Lifecycle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Pure popup presentation policy, kept separate so lifecycle races are unit-testable. */
internal object PopupPresentationPolicy {
    fun canStartLoading(isActivityAlive: Boolean): Boolean = isActivityAlive

    fun canPresent(
        isSameActivity: Boolean,
        isActivityAlive: Boolean,
        hasWindowFocus: Boolean,
        lifecycleState: Lifecycle.State?
    ): Boolean {
        return isSameActivity &&
            isActivityAlive &&
            hasWindowFocus &&
            (lifecycleState == null || lifecycleState == Lifecycle.State.RESUMED)
    }
}

internal sealed interface PopupResultDecision {
    data class LoadFailed(val error: Any) : PopupResultDecision
    data object Empty : PopupResultDecision
    data object ScreenInactive : PopupResultDecision
    data class Present(val data: ByteArray) : PopupResultDecision
}

internal object PopupResultPolicy {
    fun decide(result: ByteArray?, error: Any?, canPresent: Boolean): PopupResultDecision {
        if (error != null) return PopupResultDecision.LoadFailed(error)
        if (result == null) return PopupResultDecision.Empty
        if (!canPresent) return PopupResultDecision.ScreenInactive
        return PopupResultDecision.Present(result)
    }
}

/** Ensures a broken or duplicated network callback cannot launch more than one popup. */
internal class PopupCompletionGate {
    private val completed = AtomicBoolean(false)

    fun tryComplete(): Boolean = completed.compareAndSet(false, true)
}

/**
 * Global latest-request-wins arbiter. A newer popup request permanently makes
 * every older successful response ineligible for presentation.
 */
internal class PopupRequestArbiter {
    private val latestRequestId = AtomicLong(0)

    fun beginRequest(): Long = latestRequestId.incrementAndGet()

    fun isLatest(requestId: Long): Boolean = latestRequestId.get() == requestId
}

/** Registers launch resources atomically and rolls them back if Activity launch fails. */
internal object PopupLaunchTransaction {
    fun launch(
        registerPayload: () -> String,
        registerScope: (() -> String)?,
        startActivity: (payloadToken: String, scopeToken: String?) -> Unit,
        removePayload: (String?) -> Unit,
        removeScope: (String?) -> Unit
    ): Result<Unit> {
        var payloadToken: String? = null
        var scopeToken: String? = null

        return runCatching {
            payloadToken = registerPayload()
            scopeToken = registerScope?.invoke()
            startActivity(requireNotNull(payloadToken), scopeToken)
        }.onFailure {
            runCatching { removePayload(payloadToken) }
            runCatching { removeScope(scopeToken) }
        }
    }
}
