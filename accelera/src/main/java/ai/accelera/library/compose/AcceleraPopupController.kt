package ai.accelera.library.compose

import ai.accelera.library.Accelera
import ai.accelera.library.utils.parentActivity
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.lang.ref.WeakReference

/**
 * Page-scoped popup launcher for Compose Navigation.
 *
 * The captured [LifecycleOwner] is the current NavBackStackEntry. Consequently,
 * a response requested by destination A is rejected after navigation to B.
 */
@Stable
class AcceleraPopupController internal constructor(
    activity: Activity?,
    lifecycleOwner: LifecycleOwner
) {
    private val activityRef = activity?.let(::WeakReference)
    private val lifecycleOwnerRef = WeakReference(lifecycleOwner)

    fun show(data: ByteArray? = null) {
        val activity = activityRef?.get() ?: return
        val lifecycleOwner = lifecycleOwnerRef.get() ?: return
        runCatching { Accelera.shared.showPopup(activity, lifecycleOwner, data) }
    }
}

/** Remembers a popup controller scoped to the current Compose destination. */
@Composable
fun rememberAcceleraPopupController(): AcceleraPopupController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.parentActivity }
    return remember(activity, lifecycleOwner) {
        AcceleraPopupController(activity, lifecycleOwner)
    }
}
